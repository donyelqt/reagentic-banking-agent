package dev.reagentic.auth.web;

import dev.reagentic.auth.service.AuthService;
import dev.reagentic.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, String email, String role) {
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req.email(), req.password());
        return ApiResponse.ok(new LoginResponse(token, req.email(), "USER"));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("USER");
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", authentication.getName());
        body.put("role", role);
        return ApiResponse.ok(body);
    }
}
