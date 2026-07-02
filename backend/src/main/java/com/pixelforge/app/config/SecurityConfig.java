package com.pixelforge.app.config;

import com.pixelforge.app.auth.jwt.JwtAccessDeniedHandler;
import com.pixelforge.app.auth.jwt.JwtAuthenticationEntryPoint;
import com.pixelforge.app.auth.jwt.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    /**
     * BCrypt: hash lento con salt, estándar en Spring Security.
     * El cost por defecto (10) es razonable para 2026.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 con body JSON cuando falta auth; 403 explícito (con el mismo
                // formato) cuando el usuario está autenticado pero le falta el rol.
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas:
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // Orden importa: "/mine" y "/mine/{id}" son más específicos que
                        // el comodín "/*" de abajo y deben evaluarse antes, o quedarían
                        // públicos por error.
                        .requestMatchers(HttpMethod.GET, "/api/games/mine", "/api/games/mine/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/games", "/api/games/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/games").hasRole("DEVELOPER")
                        .requestMatchers(HttpMethod.PUT, "/api/games/*").hasRole("DEVELOPER")
                        .requestMatchers(HttpMethod.DELETE, "/api/games/*").hasRole("DEVELOPER")
                        .requestMatchers(HttpMethod.POST, "/api/games/*/cover").hasRole("DEVELOPER")
                        // Resto de /api/** requiere JWT válido.
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Insertamos nuestro filtro JWT antes del filtro de username/password
                // estándar, para que el SecurityContext ya esté poblado cuando llegue
                // la cadena de autorización.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
