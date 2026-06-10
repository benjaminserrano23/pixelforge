package com.pixelforge.app.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelforge.app.auth.dto.AuthResponse;
import com.pixelforge.app.auth.dto.LoginRequest;
import com.pixelforge.app.auth.dto.RegisterRequest;
import com.pixelforge.app.auth.dto.UserResponse;
import com.pixelforge.app.auth.exception.EmailAlreadyUsedException;
import com.pixelforge.app.auth.exception.GlobalExceptionHandler;
import com.pixelforge.app.auth.jwt.JwtService;
import com.pixelforge.app.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice del controller con MockMvc. addFilters=false evita la cadena de
 * Spring Security en el test — aquí verificamos la lógica HTTP del controller
 * y el mapeo de excepciones, no la seguridad.
 */
@WebMvcTest(controllers = AuthController.class,
            excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "pixelforge.jwt.secret=test-only-secret-32chars-minimum-test-1234",
        "pixelforge.jwt.expiration-ms=900000"
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    // Necesario porque @WebMvcTest escanea JwtAuthenticationFilter (Filter) pero
    // no JwtService (@Service). Sin este mock, la cadena de dependencias no resuelve.
    @MockBean JwtService jwtService;

    @Test
    void register_returns_200_with_token_and_user() throws Exception {
        var req = new RegisterRequest("anna@example.com", "secret123", "Anna", UserRole.PLAYER);
        var userResp = new UserResponse(1L, "anna@example.com", "Anna", UserRole.PLAYER, Instant.parse("2026-06-10T00:00:00Z"));
        when(authService.register(any(RegisterRequest.class))).thenReturn(new AuthResponse("TOKEN", userResp));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN"))
                .andExpect(jsonPath("$.user.email").value("anna@example.com"))
                .andExpect(jsonPath("$.user.role").value("PLAYER"));
    }

    @Test
    void register_returns_400_with_field_errors_when_email_is_invalid() throws Exception {
        String body = """
                {"email": "not-an-email", "password": "secret123", "displayName": "Anna", "role": "PLAYER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void register_returns_409_when_email_already_used() throws Exception {
        var req = new RegisterRequest("anna@example.com", "secret123", "Anna", UserRole.PLAYER);
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyUsedException("anna@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("email_already_used"));
    }

    @Test
    void login_returns_200_with_token() throws Exception {
        var req = new LoginRequest("anna@example.com", "secret123");
        var userResp = new UserResponse(1L, "anna@example.com", "Anna", UserRole.DEVELOPER, Instant.parse("2026-06-10T00:00:00Z"));
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("TOKEN", userResp));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN"))
                .andExpect(jsonPath("$.user.role").value("DEVELOPER"));
    }

    @Test
    void login_returns_401_when_bad_credentials() throws Exception {
        var req = new LoginRequest("anna@example.com", "wrong");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("invalid"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }
}
