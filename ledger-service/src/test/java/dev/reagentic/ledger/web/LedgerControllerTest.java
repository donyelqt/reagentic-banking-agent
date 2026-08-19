package dev.reagentic.ledger.web;

import dev.reagentic.ledger.domain.LedgerEntry;
import dev.reagentic.ledger.repository.LedgerRepository;
import dev.reagentic.ledger.service.StatementCsvRenderer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LedgerControllerTest {

    private final LedgerRepository repository = mock(LedgerRepository.class);
    private final RestClient accountClient = mock(RestClient.class);
    private final LedgerController controller = new LedgerController(repository, accountClient);

    private static LedgerEntry entry(String paymentId, String type, String signed, String balanceAfter) {
        return new LedgerEntry("acc-checking-0001", paymentId, type,
                new BigDecimal(signed), new BigDecimal(balanceAfter));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubOwnership(boolean owned) {
        RestClient.RequestHeadersUriSpec get = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec spec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        doReturn(get).when(accountClient).get();
        doReturn(spec).when(get).uri(anyString(), (Object[]) any());
        doReturn(spec).when(spec).header(anyString(), any());
        doReturn(response).when(spec).retrieve();
        if (owned) {
            doReturn(ResponseEntity.ok().build()).when(response).toBodilessEntity();
        } else {
            doThrow(HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "404 Not Found", HttpHeaders.EMPTY, new byte[0], null))
                    .when(response).toBodilessEntity();
        }
    }

    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        return request;
    }

    private static Authentication auth(String... authorities) {
        return new UsernamePasswordAuthenticationToken("user@bank.dev", null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void statementReturnsCsvWithDownloadHeaders() {
        stubOwnership(true);
        List<LedgerEntry> entries = List.of(entry("pmt-1", "DEBIT", "-50.00", "950.00"));
        when(repository.findByAccountIdOrderByCreatedAtAsc("acc-checking-0001")).thenReturn(entries);

        ResponseEntity<String> response = controller.statement(request(), "acc-checking-0001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("text/csv;charset=UTF-8"),
                response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("statement-acc-checking-0001.csv"));
        assertEquals(StatementCsvRenderer.render(entries), response.getBody());
    }

    @Test
    void statementSanitizesAccountIdInFileName() {
        stubOwnership(true);
        when(repository.findByAccountIdOrderByCreatedAtAsc("acc x/y")).thenReturn(List.of());

        ResponseEntity<String> response = controller.statement(request(), "acc x/y");

        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("statement-acc_x_y.csv"));
    }

    @Test
    void statementForUnownedAccountThrowsNotOwnedAndMapsTo404() {
        stubOwnership(false);
        when(repository.findByAccountIdOrderByCreatedAtAsc("acc-other-0001")).thenReturn(List.of());

        assertThrows(LedgerController.NotOwnedException.class,
                () -> controller.statement(request(), "acc-other-0001"));

        ResponseEntity<Map<String, Object>> denied = controller.notOwned();
        assertEquals(HttpStatus.NOT_FOUND, denied.getStatusCode());
        assertEquals("ACCOUNT_NOT_FOUND", denied.getBody().get("code"));
    }

    @Test
    void statementInternalRejectsNonEmployee() {
        assertThrows(AccessDeniedException.class,
                () -> controller.statementInternal(auth(), "acc-checking-0001"));

        ResponseEntity<Map<String, Object>> denied = controller.denied();
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertEquals("FORBIDDEN", denied.getBody().get("code"));
    }

    @Test
    void statementInternalAllowsEmployeeAndRendersAnyAccount() {
        List<LedgerEntry> entries = List.of(entry("pmt-1", "DEBIT", "-50.00", "950.00"));
        when(repository.findByAccountIdOrderByCreatedAtAsc("acc-checking-0001")).thenReturn(entries);

        ResponseEntity<String> response = controller.statementInternal(
                auth("ROLE_EMPLOYEE"), "acc-checking-0001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(StatementCsvRenderer.render(entries), response.getBody());
    }
}