package com.pixelforge.app.auth;

import com.pixelforge.app.auth.dto.AuthResponse;
import com.pixelforge.app.auth.dto.LoginRequest;
import com.pixelforge.app.auth.dto.RegisterRequest;
import com.pixelforge.app.auth.dto.UserResponse;
import com.pixelforge.app.auth.exception.EmailAlreadyUsedException;
import com.pixelforge.app.auth.jwt.JwtService;
import com.pixelforge.app.user.User;
import com.pixelforge.app.user.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyUsedException(req.email());
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setDisplayName(req.displayName());
        user.setRole(req.role());
        user = userRepository.save(user);
        return new AuthResponse(jwtService.issue(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Mensaje genérico en ambos caminos (no se filtra si el email existe o no).
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("invalid credentials");
        }
        return new AuthResponse(jwtService.issue(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        // Si el token está bien firmado pero el usuario fue borrado, devolvemos
        // 401 (BadCredentials) en vez de 404 para no filtrar existencia.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("user not found"));
        return UserResponse.from(user);
    }
}
