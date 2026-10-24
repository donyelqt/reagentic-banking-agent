package dev.reagentic.auth.web;

import dev.reagentic.auth.domain.User;
import dev.reagentic.auth.repository.UserRepository;
import dev.reagentic.auth.service.AuthService;
import dev.reagentic.auth.service.PasswordChangeException;
import dev.reagentic.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileControllerTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserRepository userRepository;
    private ProfileController controller;
    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        controller = new ProfileController(userRepository);
        user = new User("usr-demo-0001", "demo@bank.dev", passwordEncoder.encode("demo1234"), "USER");
        when(userRepository.findByEmail("demo@bank.dev")).thenReturn(Optional.of(user));
        authentication = new UsernamePasswordAuthenticationToken("demo@bank.dev", null);
    }

    @Test
    void returnsTheProfileForTheAuthenticatedSubject() {
        user.setFullName("Dani Reyes");
        user.setPhone("+1 (312) 847-1928");

        ProfileController.ProfileView profile = controller.getProfile(authentication).data();

        assertEquals("demo@bank.dev", profile.email());
        assertEquals("USER", profile.role());
        assertEquals("Dani Reyes", profile.fullName());
        assertEquals("+1 (312) 847-1928", profile.phone());
    }

    @Test
    void updatesProfileFieldsAndPersistsThem() {
        ApiResponse<ProfileController.ProfileView> response = controller.updateProfile(
                new ProfileController.UpdateProfileRequest("  Dani Reyes  ", " +1 (312) 847-1928 "),
                authentication);

        assertEquals("Dani Reyes", response.data().fullName());
        assertEquals("+1 (312) 847-1928", response.data().phone());
        assertEquals("Dani Reyes", user.getFullName());
        assertEquals("+1 (312) 847-1928", user.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    void blankProfileFieldsAreStoredAsNull() {
        user.setFullName("Dani Reyes");
        user.setPhone("+1 (312) 847-1928");

        controller.updateProfile(new ProfileController.UpdateProfileRequest("   ", ""), authentication);

        assertNull(user.getFullName());
        assertNull(user.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    void rejectsAnAuthenticatedSubjectThatNoLongerExists() {
        when(userRepository.findByEmail("ghost@bank.dev")).thenReturn(Optional.empty());
        Authentication ghost = new UsernamePasswordAuthenticationToken("ghost@bank.dev", null);

        assertThrows(AuthService.AuthException.class, () -> controller.getProfile(ghost));
    }

    @Test
    void changesThePasswordWhenTheCurrentPasswordMatches() {
        ApiResponse<String> response = controller.changePassword(
                new ProfileController.ChangePasswordRequest("demo1234", "newpass456"),
                authentication);

        assertTrue(response.success());
        assertEquals("Password updated", response.message());
        assertTrue(passwordEncoder.matches("newpass456", user.getPasswordHash()));
        verify(userRepository).save(user);
    }

    @Test
    void rejectsAWrongCurrentPassword() {
        String originalHash = user.getPasswordHash();

        PasswordChangeException failure = assertThrows(PasswordChangeException.class,
                () -> controller.changePassword(
                        new ProfileController.ChangePasswordRequest("not-the-password", "newpass456"),
                        authentication));

        assertEquals("INVALID_CURRENT_PASSWORD", failure.getCode());
        assertEquals(originalHash, user.getPasswordHash());
        verify(userRepository, never()).save(user);
    }

    @Test
    void rejectsReusingTheCurrentPassword() {
        String originalHash = user.getPasswordHash();

        PasswordChangeException failure = assertThrows(PasswordChangeException.class,
                () -> controller.changePassword(
                        new ProfileController.ChangePasswordRequest("demo1234", "demo1234"),
                        authentication));

        assertEquals("PASSWORD_REUSE", failure.getCode());
        assertEquals(originalHash, user.getPasswordHash());
        verify(userRepository, never()).save(user);
    }

    @Test
    void rejectsAPasswordBeyondBcryptsByteLimit() {
        String multibytePassword = "\uD83D\uDD12".repeat(19);

        PasswordChangeException failure = assertThrows(PasswordChangeException.class,
                () -> controller.changePassword(
                        new ProfileController.ChangePasswordRequest("demo1234", multibytePassword),
                        authentication));

        assertEquals("PASSWORD_TOO_LONG", failure.getCode());
        verify(userRepository, never()).save(user);
    }
}
