package dev.reagentic.auth.web;

import dev.reagentic.auth.domain.User;
import dev.reagentic.auth.repository.UserRepository;
import dev.reagentic.auth.service.AuthService;
import dev.reagentic.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/profile")
public class ProfileController {

    private final UserRepository userRepository;

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

    @GetMapping
    public ApiResponse<ProfileView> getProfile(Authentication authentication) {
        return ApiResponse.ok(toView(requireUser(authentication.getName())));
    }

    @PutMapping
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
