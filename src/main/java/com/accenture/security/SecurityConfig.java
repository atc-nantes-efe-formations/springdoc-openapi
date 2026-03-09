package com.accenture.security;

import com.accenture.dto.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security de l'API.
 *
 * <p>Règles d'accès globales :</p>
 * <ul>
 *   <li>GET  sur {@code /api/**}              → public (aucune authentification requise)</li>
 *   <li>Tout autre verbe sur {@code /api/**}  → authentification requise</li>
 *   <li>Swagger UI et API docs                → toujours publics</li>
 * </ul>
 *
 * <p>Les autorisations fines (rôles) sont déclarées directement
 * sur chaque méthode de {@code TaskApi} via {@code @PreAuthorize}.</p>
 *
 * <p>Utilisateurs en mémoire :</p>
 * <ul>
 *   <li>{@code user}  / {@code user123}  → rôle USER</li>
 *   <li>{@code admin} / {@code admin123} → rôle ADMIN</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // active @PreAuthorize / @PostAuthorize sur les méthodes
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // Filtre de sécurité
    // =========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter) throws Exception {
        http
                // Désactive CSRF — API REST stateless, pas de session navigateur
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // Swagger UI et spec OpenAPI — toujours accessibles
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs.yaml",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // GET /api/** — lecture publique
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                        // Tout autre verbe — authentification requise
                        // (les autorisations par rôle sont gérées via @PreAuthorize dans TaskApi)
                        .anyRequest().authenticated()
                )

                // Authentification HTTP Basic (bouton Authorize dans Swagger UI)
                .httpBasic(Customizer.withDefaults())
                // Ajout du support Bearer token
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Pour éviter les redirections HTML
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            ErrorDto error = ErrorDto.of(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHORIZED",
                                    authException.getMessage(),
                                    request.getRequestURI()
                            );
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getWriter(), error);
                        })
                )
        ;

        return http.build();
    }

    // =========================================================================
    // Utilisateurs en mémoire
    // =========================================================================

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username("user")
                        .password(encoder.encode("user123"))
                        .roles("USER")
                        .build(),
                User.builder()
                        .username("admin")
                        .password(encoder.encode("admin123"))
                        .roles("ADMIN")
                        .build()
        );
    }

    // =========================================================================
    // Encodeur de mots de passe
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public TokenAuthenticationService tokenAuthenticationService() {
        return new TokenAuthenticationService();
    }


    @Bean
    public FilterRegistrationBean<BearerTokenAuthenticationFilter> bearerTokenFilterRegistration(BearerTokenAuthenticationFilter filter) {
        FilterRegistrationBean<BearerTokenAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // désactive l'enregistrement auto sur la servlet
        return registration;
    }
}
