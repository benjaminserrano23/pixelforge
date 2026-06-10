package com.pixelforge.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Configuración mínima para que la API sea accesible sin login
     * mientras montamos el esqueleto. Cuando agreguemos autenticación
     * real, este es el único punto a tocar.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF está pensado para sesiones de navegador con cookies.
                // Nuestra API es stateless, así que se desactiva.
                .csrf(csrf -> csrf.disable())
                // Sin sesión HTTP: cada request se autentica por sí mismo (cuando toque).
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
