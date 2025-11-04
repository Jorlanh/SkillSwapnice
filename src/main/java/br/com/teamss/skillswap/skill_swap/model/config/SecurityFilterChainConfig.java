package br.com.teamss.skillswap.skill_swap.model.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Manter para o RegisterController se ele for adaptado
import org.springframework.security.crypto.password.PasswordEncoder; // Manter
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Essencial para @PreAuthorize
public class SecurityFilterChainConfig {

    @Value("${cors.allowed-origins:http://localhost:4200,https://skillswap-frontend-tmub.onrender.com}")
    private String[] allowedOrigins;

    // REMOVIDO: A injeção do JwtAuthenticationFilter
    // public SecurityFilterChainConfig(JwtAuthenticationFilter jwtAuthenticationFilter) { ... }

    @Bean
    @Order(1)
    @Profile("dev")
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        // ... (seu código H2-console continua igual)
        http
            .securityMatcher(AntPathRequestMatcher.antMatcher("/h2-console/**"))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")))
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF pode ser desabilitado para uma API REST stateless
            .csrf(csrf -> csrf.disable())
            
            // ATUALIZADO: Headers de segurança (removido o CSP antigo que não é mais necessário)
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .frameOptions(frameOptions -> frameOptions.deny())
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            
            // Sessão STATELESS, pois cada requisição é autenticada pelo token
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Regras de autorização
            .authorizeHttpRequests(auth -> auth
                .requestMatchers( // Endpoints públicos
                    // O Register e PasswordReset são removidos daqui, pois o Auth0 os gerencia
                    // AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/register"),
                    // AntPathRequestMatcher.antMatcher("/api/verify"),
                    // AntPathRequestMatcher.antMatcher("/api/password-reset/**"),
                    
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/skills/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/roles/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/search/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/profiles/user/{username}"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/rankings/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS, "/**"), // Permite requisições CORS preflight
                    AntPathRequestMatcher.antMatcher("/error")
                ).permitAll()
                // Todas as outras requisições exigem autenticação
                .anyRequest().authenticated()
            )
            
            // REMOVIDO: .addFilterBefore(jwtAuthenticationFilter, ...)
            
            // A MÁGICA DO OAUTH 2.0: Configura a app como um Resource Server que valida JWTs
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Mesmo que o login seja externo, manter o PasswordEncoder é útil
        // caso você precise de hashes para outras coisas (ex: chaves de API).
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Cache-Control", "Content-Type", "X-Requested-With",
            "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Rate-Limit-Remaining"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}