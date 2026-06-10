package com.pixelforge.app.auth;

import com.pixelforge.app.auth.dto.AuthResponse;
import com.pixelforge.app.auth.dto.LoginRequest;
import com.pixelforge.app.auth.dto.RegisterRequest;
import com.pixelforge.app.auth.dto.UserResponse;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /**
     * Devuelve el perfil del usuario asociado al JWT. El email viene del principal
     * que pobló JwtAuthenticationFilter cuando validó el token.
     */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
