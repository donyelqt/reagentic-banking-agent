package dev.reagentic.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AgentWorkers {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkers.class);

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

    public String getBalance(String token, String accountId) {
        JsonNode data = getNode(accountClient, "/api/accounts/" + accountId + "/balance", token);
        return data.get("balance").asText();
    }

    public JsonNode listTransactions(String token, String accountId) {
        return getNode(ledgerClient, "/api/ledger/" + accountId, token);
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
        String balance = getBalance(token, accountId);
        JsonNode txns = listTransactions(token, accountId);
        BigDecimal sum = BigDecimal.ZERO;
        if (txns != null && txns.isArray()) {
            for (JsonNode t : txns) {
                sum = sum.add(new BigDecimal(t.get("signedAmount").asText("0")));
            }
        }
        BigDecimal bal = new BigDecimal(balance);
        boolean balanced = sum.compareTo(bal) == 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", accountId);
        result.put("balance", balance);
        result.put("ledgerSum", sum.toPlainString());
        result.put("balanced", balanced);
        if (!balanced) {
            result.put("delta", bal.subtract(sum).toPlainString());
            result.put("suspect", sum.compareTo(bal) < 0 ? "MISSING_DEBIT_LEG" : "MISSING_CREDIT_LEG");
            result.put("diagnosis",
                    "Account balance and the immutable ledger disagree. A ledger leg is missing.");
        }
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
