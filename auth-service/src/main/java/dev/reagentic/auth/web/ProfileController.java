package dev.reagentic.auth.web;

import dev.reagentic.auth.domain.User;
import dev.reagentic.auth.repository.UserRepository;
import dev.reagentic.auth.service.AuthService;
import dev.reagentic.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class ProfileController {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record ProfileView(String email, String role, String fullName, String phone) {
    }

    public record UpdateProfileRequest(
            @Size(max = 120, message = "Full name must be 120 characters or fewer") String fullName,
            @Size(max = 32, message = "Phone must be 32 characters or fewer") String phone
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters") String newPassword
    ) {
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileView> getProfile(Authentication authentication) {
        return ApiResponse.ok(toView(requireUser(authentication.getName())));
    }

    @PutMapping("/profile")
    @Transactional
    public ApiResponse<ProfileView> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        User user = requireUser(authentication.getName());
        user.setFullName(clean(request.fullName()));
        user.setPhone(clean(request.phone()));
        userRepository.save(user);
        return ApiResponse.ok(toView(user));
    }

    @PutMapping("/password")
    @Transactional
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        User user = requireUser(authentication.getName());

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Current password is incorrect"));
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("New password must be different from the current password"));
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("New password is too long"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Password updated"));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthService.AuthException("USER_NOT_FOUND", "Authenticated user no longer exists"));
    }

    private static ProfileView toView(User user) {
        return new ProfileView(user.getEmail(), user.getRole(), user.getFullName(), user.getPhone());
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
