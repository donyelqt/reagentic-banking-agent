package dev.reagentic.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentWorkers {

    private final RestClient accountClient;
    private final RestClient ledgerClient;
    private final RestClient paymentClient;
    private final ObjectMapper objectMapper;

    public AgentWorkers(RestClient accountClient, RestClient ledgerClient,
                        RestClient paymentClient, ObjectMapper objectMapper) {
        this.accountClient = accountClient;
        this.ledgerClient = ledgerClient;
        this.paymentClient = paymentClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode listAccounts(String token) {
        return getNode(accountClient, "/api/accounts", token);
    }

    public String getBalance(String token, String accountId, boolean internal) {
        String path = internal
                ? "/api/accounts/internal/balance/" + accountId
                : "/api/accounts/" + accountId + "/balance";
        JsonNode data = getNode(accountClient, path, token);
        return data.get("balance").asText();
    }

    public JsonNode listTransactions(String token, String accountId, boolean internal) {
        String path = internal ? "/api/ledger/internal/" + accountId : "/api/ledger/" + accountId;
        return getNode(ledgerClient, path, token);
    }

    public JsonNode transfer(String token, String from, String to, String amount, String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceAccountId", from);
        body.put("destinationAccountId", to);
        body.put("amount", amount);
        body.put("idempotencyKey", idempotencyKey);
        try {
            String resp = paymentClient.post()
                    .uri("/api/payments/transfer")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return dataOf(resp);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("transfer failed: " + extractMessage(e.getResponseBodyAsString()));
        }
    }

    public Map<String, Object> reconcile(String token, String accountId) {
        String balance = getBalance(token, accountId, true);
        JsonNode txns = listTransactions(token, accountId, true);
        BigDecimal sum = BigDecimal.ZERO;
        List<Map<String, Object>> evidence = new ArrayList<>();
        String lastEntryId = null, lastPaymentId = null, lastType = null, lastBalanceAfter = null;
        if (txns != null && txns.isArray()) {
            for (JsonNode t : txns) {
                String signed = t.path("signedAmount").asText("0");
                sum = sum.add(new BigDecimal(signed));
                lastEntryId = t.path("entryId").asText();
                lastPaymentId = t.hasNonNull("paymentId") ? t.path("paymentId").asText() : "OPENING";
                lastType = t.path("type").asText();
                lastBalanceAfter = t.path("balanceAfter").asText();
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("entryId", t.path("entryId").asText());
                e.put("type", lastType);
                e.put("signedAmount", signed);
                e.put("balanceAfter", lastBalanceAfter);
                e.put("paymentId", t.hasNonNull("paymentId") ? t.path("paymentId").asText() : "OPENING");
                evidence.add(e);
            }
        }
        BigDecimal bal = new BigDecimal(balance);
        BigDecimal delta = bal.subtract(sum);
        boolean balanced = delta.compareTo(BigDecimal.ZERO) == 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", accountId);
        result.put("balance", balance);
        result.put("ledgerSum", sum.toPlainString());
        result.put("balanced", balanced);
        if (balanced) {
            return result;
        }
        BigDecimal lastBal = (lastBalanceAfter == null || lastBalanceAfter.isBlank())
                ? BigDecimal.ZERO : new BigDecimal(lastBalanceAfter);
        BigDecimal missingSigned = bal.subtract(lastBal);
        String direction = missingSigned.signum() < 0 ? "MISSING_DEBIT_LEG" : "MISSING_CREDIT_LEG";
        BigDecimal missingAmount = missingSigned.abs();
        String anchorPayment = (lastPaymentId == null || lastPaymentId.isBlank()) ? "OPENING" : lastPaymentId;
        result.put("delta", delta.toPlainString());
        result.put("direction", direction);
        result.put("missingAmount", missingAmount.toPlainString());
        result.put("lastEntryId", lastEntryId);
        result.put("lastPaymentId", lastPaymentId);
        result.put("lastBalanceAfter", lastBalanceAfter);
        result.put("diagnosis",
                "Account balance (" + balance + ") does not equal the immutable ledger. The ledger is "
                + "internally consistent and ends at balanceAfter=" + lastBalanceAfter + " (entry #" + lastEntryId
                + ", payment " + anchorPayment + "), but the account now reports " + balance + ". A "
                + direction + " of " + missingAmount.toPlainString() + " was applied to the account but "
                + "was never appended to the ledger. Investigate the transfer/payment that should have "
                + "written this leg.");
        int from = Math.max(0, evidence.size() - 12);
        result.put("evidenceCount", evidence.size());
        result.put("evidence", evidence.subList(from, evidence.size()));
        return result;
    }

    private JsonNode getNode(RestClient client, String path, String token) {
        try {
            String resp = client.get()
                    .uri(path)
                    .header("Authorization", token)
                    .retrieve()
                    .body(String.class);
            return dataOf(resp);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("backend call " + path + " failed: "
                    + extractMessage(e.getResponseBodyAsString()));
        }
    }

    private JsonNode dataOf(String resp) {
        try {
            JsonNode root = objectMapper.readTree(resp);
            if (root.has("success") && !root.get("success").asBoolean(false)) {
                throw new RuntimeException(root.path("message").asText("backend error"));
            }
            return root.get("data");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("failed to parse backend response", e);
        }
    }

    private String extractMessage(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("message")) {
                return node.get("message").asText();
            }
            if (node.has("error")) {
                return node.get("error").asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }
}
