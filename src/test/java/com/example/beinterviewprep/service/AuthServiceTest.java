package com.example.beinterviewprep.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.dto.LoginRequest;
import com.example.beinterviewprep.dto.RegisterRequest;
import com.example.beinterviewprep.dto.TokenResponse;
import com.example.beinterviewprep.dto.UserResponse;
import com.example.beinterviewprep.entity.AppUser;
import com.example.beinterviewprep.entity.Role;
import com.example.beinterviewprep.exception.AuthenticationFailedException;
import com.example.beinterviewprep.exception.BusinessRuleException;
import com.example.beinterviewprep.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private AuthService service() {
        return new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private AppUser storedUser(String email, String rawPassword, Role role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return user;
    }

    @Test
    void registerStoresHashedPasswordAndUserRole() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service().register(new RegisterRequest("  Jane@Example.com ", "s3cret-pass"));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertEquals("jane@example.com", saved.getValue().getEmail());
        assertEquals(Role.USER, response.role());
        assertNotEquals("s3cret-pass", saved.getValue().getPasswordHash());
        assertEquals(true, passwordEncoder.matches("s3cret-pass", saved.getValue().getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(
                BusinessRuleException.class,
                () -> service().register(new RegisterRequest("jane@example.com", "s3cret-pass")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithCorrectPasswordReturnsBearerToken() {
        AppUser user = storedUser("jane@example.com", "s3cret-pass", Role.USER);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("signed-token");
        when(jwtService.expiresInSeconds()).thenReturn(900L);

        TokenResponse response = service().login(new LoginRequest("Jane@example.com", "s3cret-pass"));

        assertEquals("signed-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresInSeconds());
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        AppUser user = storedUser("jane@example.com", "s3cret-pass", Role.USER);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThrows(
                AuthenticationFailedException.class,
                () -> service().login(new LoginRequest("jane@example.com", "wrong-pass")));
    }

    @Test
    void loginWithUnknownEmailIsRejected() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(
                AuthenticationFailedException.class,
                () -> service().login(new LoginRequest("ghost@example.com", "s3cret-pass")));
    }
}
