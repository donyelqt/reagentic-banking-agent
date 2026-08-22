package dev.reagentic.auth.web;

import dev.reagentic.auth.domain.User;
import dev.reagentic.auth.repository.UserRepository;
import dev.reagentic.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record UpdateProfileRequest(String fullName, String phone) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6, message = "New password must be at least 6 characters") String newPassword
    ) {}

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("role", user != null ? user.getRole() : "USER");
        response.put("fullName", user != null && user.getFullName() != null ? user.getFullName() : (email.startsWith("ops") ? "Ops Analyst" : "Demo Customer"));
        response.put("phone", user != null && user.getPhone() != null ? user.getPhone() : "+1 (555) 019-2834");
        response.put("twoFactorEnabled", false);
        response.put("theme", "system");
        response.put("defaultAccount", "acc-checking-0001");
        response.put("currency", "USD");
        
        return ApiResponse.ok(response);
    }

    @PutMapping("/profile")
    @Transactional
    public ApiResponse<Map<String, Object>> updateProfile(
            @RequestBody UpdateProfileRequest req,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.phone() != null) user.setPhone(req.phone());
        userRepository.save(user);

        return getProfile(authentication);
    }

    @PutMapping("/password")
    @Transactional
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Current password is incorrect"));
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully"));
    }
}
