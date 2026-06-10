package com.pixelforge.app.auth;

import com.pixelforge.app.auth.dto.AuthResponse;
import com.pixelforge.app.auth.dto.LoginRequest;
import com.pixelforge.app.auth.dto.RegisterRequest;
import com.pixelforge.app.auth.exception.EmailAlreadyUsedException;
import com.pixelforge.app.auth.jwt.JwtService;
import com.pixelforge.app.user.User;
import com.pixelforge.app.user.UserRepository;
import com.pixelforge.app.user.UserRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    @Test
    void register_creates_user_hashes_password_and_returns_token() {
        var req = new RegisterRequest("anna@example.com", "secret123", "Anna", UserRole.PLAYER);
        when(userRepository.existsByEmail("anna@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.issue(any(User.class))).thenReturn("TOKEN");

        AuthResponse res = authService.register(req);

        assertThat(res.token()).isEqualTo("TOKEN");
        assertThat(res.user().email()).isEqualTo("anna@example.com");
        assertThat(res.user().displayName()).isEqualTo("Anna");
        assertThat(res.user().role()).isEqualTo(UserRole.PLAYER);
        // El service nunca debería volver a llamar a encode con el password en claro:
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void register_throws_when_email_already_exists() {
        var req = new RegisterRequest("anna@example.com", "secret123", "Anna", UserRole.PLAYER);
        when(userRepository.existsByEmail("anna@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).issue(any());
    }

    @Test
    void login_succeeds_with_correct_credentials() {
        User user = sampleUser();
        when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "HASH")).thenReturn(true);
        when(jwtService.issue(user)).thenReturn("TOKEN");

        AuthResponse res = authService.login(new LoginRequest("anna@example.com", "secret123"));

        assertThat(res.token()).isEqualTo("TOKEN");
        assertThat(res.user().role()).isEqualTo(UserRole.DEVELOPER);
    }

    @Test
    void login_throws_bad_credentials_when_password_does_not_match() {
        User user = sampleUser();
        when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("anna@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).issue(any());
    }

    @Test
    void login_throws_bad_credentials_when_email_unknown() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "secret123")))
                .isInstanceOf(BadCredentialsException.class);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void me_returns_user_response_when_user_exists() {
        when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(sampleUser()));

        var resp = authService.me("anna@example.com");

        assertThat(resp.email()).isEqualTo("anna@example.com");
        assertThat(resp.role()).isEqualTo(UserRole.DEVELOPER);
    }

    @Test
    void me_throws_bad_credentials_when_user_no_longer_exists() {
        when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("anna@example.com"))
                .isInstanceOf(BadCredentialsException.class);
    }

    private User sampleUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail("anna@example.com");
        u.setPasswordHash("HASH");
        u.setDisplayName("Anna");
        u.setRole(UserRole.DEVELOPER);
        u.setCreatedAt(Instant.now());
        return u;
    }
}
