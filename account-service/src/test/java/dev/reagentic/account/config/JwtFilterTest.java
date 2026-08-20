package dev.reagentic.account.config;

import dev.reagentic.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtFilterTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    private JwtFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication runFilter(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<Authentication> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> captured.set(SecurityContextHolder.getContext().getAuthentication());
        filter.doFilter(request, response, chain);
        return captured.get();
    }

    private MockHttpServletRequest debitRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/accounts/internal/debit");
        request.setRequestURI("/api/accounts/internal/debit");
        return request;
    }

    @Test
    void signedServiceJwtGrantsServiceRole() throws Exception {
        MockHttpServletRequest request = debitRequest();
        request.addHeader("Authorization", "Bearer " + JwtUtil.issue(SECRET, "user1", "SERVICE", 60_000));
        // Legacy headers are now ignored entirely - include them to prove it.
        request.addHeader("X-Service-Token", "anything");
        request.addHeader("X-User-Subject", "attacker-chosen-user");

        Authentication auth = runFilter(request, new MockHttpServletResponse());

        assertNotNull(auth);
        assertTrue(auth instanceof UsernamePasswordAuthenticationToken);
        assertEquals("user1", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE")));
    }

    @Test
    void userJwtGrantsUserRoleEvenWithServiceTokenHeaders() throws Exception {
        MockHttpServletRequest request = debitRequest();
        request.addHeader("Authorization", "Bearer " + JwtUtil.issue(SECRET, "user1", "USER", 60_000));
        request.addHeader("X-Service-Token", "whatever");
        request.addHeader("X-User-Subject", "someone-else");

        Authentication auth = runFilter(request, new MockHttpServletResponse());

        assertNotNull(auth);
        assertEquals("user1", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_SERVICE")));
    }

    @Test
    void staticServiceTokenHeaderWithoutJwtIsRejected() throws Exception {
        MockHttpServletRequest request = debitRequest();
        request.addHeader("X-Service-Token", "svc-token-reagentic-demo-local");
        request.addHeader("X-User-Subject", "user1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        runFilter(request, response);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        MockHttpServletRequest request = debitRequest();
        request.addHeader("Authorization", "Bearer not.a.valid.jwt");

        MockHttpServletResponse response = new MockHttpServletResponse();
        runFilter(request, response);

        assertEquals(401, response.getStatus());
    }
}