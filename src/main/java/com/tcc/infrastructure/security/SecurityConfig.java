package com.tcc.infrastructure.security;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ServiceKeyAuthFilter serviceKeyAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          ServiceKeyAuthFilter serviceKeyAuthFilter,
                          UserDetailsServiceImpl userDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.serviceKeyAuthFilter = serviceKeyAuthFilter;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints de /auth/** que exigem autenticação — declarados antes
                        // do permitAll de /auth/** para que o Spring pare no match correto.
                        .requestMatchers("/auth/me", "/auth/change-password").authenticated()
                        .requestMatchers(
                                "/auth/**",
                                "/forgot-password/**",
                                "/account-activation/**",
                                "/actuator/health",
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()
                        // Integração serviço a serviço: autenticada pelo ServiceKeyAuthFilter
                        // via header X-Integration-Key. Nunca permitAll.
                        .requestMatchers("/api/integration/**").hasAuthority("ROLE_INTEGRATION")
                        .requestMatchers("/api/doctors/**").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/patients/**").hasAnyRole("ADMIN", "HOSPITAL", "DOCTOR")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/hospital/**").hasRole("HOSPITAL")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("timestamp", LocalDateTime.now().toString());
                            body.put("status", 401);
                            body.put("error", "Unauthorized");
                            body.put("message", "Token ausente, inválido ou expirado");
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("timestamp", LocalDateTime.now().toString());
                            body.put("status", 403);
                            body.put("error", "Forbidden");
                            body.put("message", "Acesso negado: você não tem permissão para este recurso");
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )
                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                )
                .authenticationProvider(authenticationProvider())
                // O filtro de chave de serviço vem antes do JWT: ele só atua em
                // /api/integration/**, onde nunca há JWT, e assim uma chamada de
                // integração não passa pelo processamento de token à toa. Nas demais
                // rotas ele se auto-ignora e o JwtAuthFilter segue sendo o único a
                // autenticar.
                .addFilterBefore(serviceKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
