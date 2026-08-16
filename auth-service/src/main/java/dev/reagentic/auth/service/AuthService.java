package dev.reagentic.auth.service;

import dev.reagentic.auth.config.JwtService;
import dev.reagentic.auth.domain.User;
import dev.reagentic.auth.repository.UserRepository;
import dev.reagentic.common.DemoConstants;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", "Invalid email or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException("INVALID_CREDENTIALS", "Invalid email or password");
        }
        return new LoginResult(jwtService.issue(user.getEmail(), user.getRole()), user.getRole());
    }

    public record LoginResult(String token, String role) {
    }

    public void seedDemoUserIfAbsent() {
        if (userRepository.findByEmail(DemoConstants.DEMO_USER_EMAIL).isEmpty()) {
            userRepository.save(new User(
                    DemoConstants.DEMO_USER_ID,
                    DemoConstants.DEMO_USER_EMAIL,
                    passwordEncoder.encode(DemoConstants.DEMO_USER_PASSWORD),
                    DemoConstants.DEMO_USER_ROLE));
        }
    }

    public void seedEmployeeIfAbsent() {
        if (userRepository.findByEmail(DemoConstants.EMPLOYEE_USER_EMAIL).isEmpty()) {
            userRepository.save(new User(
                    DemoConstants.EMPLOYEE_USER_ID,
                    DemoConstants.EMPLOYEE_USER_EMAIL,
                    passwordEncoder.encode(DemoConstants.EMPLOYEE_USER_PASSWORD),
                    DemoConstants.EMPLOYEE_USER_ROLE));
        }
    }

    public static class AuthException extends RuntimeException {
        private final String code;

        public AuthException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
