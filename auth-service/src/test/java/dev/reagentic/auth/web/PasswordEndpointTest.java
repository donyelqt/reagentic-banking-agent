package dev.reagentic.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.auth.config.JwtFilter;
import dev.reagentic.auth.config.SecurityConfig;
import dev.reagentic.auth.domain.User;
import dev.reagentic.auth.repository.UserRepository;
import dev.reagentic.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "JWT_SECRET=" + PasswordEndpointTest.SECRET)
class PasswordEndpointTest {

    static final String SECRET = "test-secret-for-auth-service-integration-32";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        user = new User("usr-demo-0001", "demo@bank.dev", passwordEncoder.encode("demo1234"), "USER");
        when(userRepository.findByEmail("demo@bank.dev")).thenReturn(Optional.of(user));
        token = JwtUtil.issue(SECRET, "demo@bank.dev", "USER", 60_000);
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("demo1234", "newpass456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MISSING_TOKEN"));
    }

    @Test
    void rejectsAShortPasswordThroughValidation() throws Exception {
        mockMvc.perform(authenticatedPut("demo1234", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void rejectsAWrongCurrentPasswordWithTheErrorEnvelope() throws Exception {
        mockMvc.perform(authenticatedPut("not-the-password", "newpass456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void changesThePassword() throws Exception {
        mockMvc.perform(authenticatedPut("demo1234", "newpass456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password updated"));

        assertTrue(passwordEncoder.matches("newpass456", user.getPasswordHash()));
    }

    @Test
    void acceptsAPasswordAtBcryptsByteLimit() throws Exception {
        mockMvc.perform(authenticatedPut("demo1234", "\uD83D\uDD12".repeat(18)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAPasswordBeyondBcryptsByteLimit() throws Exception {
        mockMvc.perform(authenticatedPut("demo1234", "\uD83D\uDD12".repeat(19)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_TOO_LONG"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedPut(
            String currentPassword, String newPassword) throws Exception {
        return put("/api/auth/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(currentPassword, newPassword));
    }

    private byte[] body(String currentPassword, String newPassword) throws Exception {
        return objectMapper.writeValueAsBytes(Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword));
    }
}
