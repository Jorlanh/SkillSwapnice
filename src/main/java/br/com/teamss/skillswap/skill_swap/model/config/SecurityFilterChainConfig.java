package br.com.teamss.skillswap.skill_swap.model.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Import for @PreAuthorize
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
// Removed UserDetailsService import as it's not directly used here for config
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler; // Handler for CSRF token inclusion
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable method-level security like @PreAuthorize
public class SecurityFilterChainConfig {

    @Value("${cors.allowed-origins:http://localhost:4200,https://skillswap-frontend-tmub.onrender.com}")
    private String[] allowedOrigins;

    // Removed UserDetailsService and JwtTokenUtil as they are injected into JwtAuthenticationFilter
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityFilterChainConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        // Constructor injection for the filter
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(1) // Keep H2 console filter first if using dev profile
    @Profile("dev")
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(AntPathRequestMatcher.antMatcher("/h2-console/**"))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")))
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // Allow framing for H2 console
        return http.build();
    }

    @Bean
    @Order(2) // Main security filter chain
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // Set the name of the attribute the CsrfToken will be populated on
        requestHandler.setCsrfRequestAttributeName(null); // Setting to null uses the default _csrf

        http
            // CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF Configuration (using cookies, suitable for SPAs)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler) // Use the handler
                // Optionally ignore CSRF for specific paths if needed (e.g., stateless APIs if mixing approaches)
                // .ignoringRequestMatchers("/api/stateless/**")
            )
            // Security Headers
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts // Enforce HTTPS
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentSecurityPolicy(csp -> csp // Content Security Policy
                    .policyDirectives("default-src 'self'; " +
                                      "script-src 'self' https://vlibras.gov.br https://player.handtalk.me; " + // Allow scripts from VLibras/HandTalk
                                      "style-src 'self' https://fonts.googleapis.com 'unsafe-inline'; " + // Allow Google Fonts and potentially inline styles if needed
                                      "font-src 'self' https://fonts.gstatic.com; " +
                                      "img-src 'self' data: https:; " + // Allow images from self, data URIs, and any HTTPS source
                                      "object-src 'none'; " + // Disallow <object>, <embed>
                                      "connect-src 'self' https://api.vlibras.gov.br wss://*.handtalk.me; " + // Allow connections to self, VLibras API, HandTalk WS
                                      "frame-src https://vlibras.gov.br https://player.handtalk.me; " + // Allow framing VLibras/HandTalk widgets
                                      "frame-ancestors 'none'; " + // Disallow embedding in frames/iframes
                                      "form-action 'self'; " + // Forms should only submit to self
                                      "base-uri 'self';")) // Restrict <base> tag
                .frameOptions(frameOptions -> frameOptions.deny()) // Prevent clickjacking (DENY except where overridden by CSP frame-src)
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)) // Enable basic XSS protection (though CSP is stronger)
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)) // Control referrer information
                .permissionsPolicy(permissions -> permissions.policy("geolocation=(), microphone=(), camera=()")) // Example Permissions-Policy
            )
            // Session Management: Stateless because we use JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Request Authorization Rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers( // Public endpoints
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/login"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/register"),
                    AntPathRequestMatcher.antMatcher("/api/verify"),
                    AntPathRequestMatcher.antMatcher("/api/password-reset/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/skills/**"), // Allow fetching skills publicly
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/roles/**"),   // Allow fetching roles publicly (if needed)
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/search/**"), // Public search
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/**"),    // Public home feed elements
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/profiles/user/{username}"), // Public profile view
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/rankings/**"), // Public rankings view
                    AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS, "/**"), // Allow CORS preflight requests
                    // Allow WebSocket handshake endpoint if not secured by JWT query param initially
                    // AntPathRequestMatcher.antMatcher("/video-call/**"), // Adjust if securing WS differently
                    AntPathRequestMatcher.antMatcher("/error") // Allow error page
                ).permitAll()
                 // Secure specific endpoints (examples)
                .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.PUT, "/api/users/{id}")).authenticated() // User update needs auth
                .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/users/{userId}/skills")).hasRole("ADMIN") // Only ADMIN can add skills like this
                .requestMatchers(AntPathRequestMatcher.antMatcher("/console-management/**")).hasRole("ADMIN") // Secure admin console
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/settings/**")).authenticated() // User settings require auth
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/chatbot/ask")).authenticated() // Chatbot requires auth
                // Default rule: All other requests must be authenticated
                .anyRequest().authenticated()
            )
            // Add JWT Filter before the standard Username/Password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowedOrigins array directly
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // Added PATCH
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Cache-Control", "Content-Type", "X-Requested-With",
            "X-XSRF-TOKEN", // Include CSRF header if needed by frontend
            "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Rate-Limit-Remaining", "X-XSRF-TOKEN" // Expose CSRF token header if needed
        ));
        configuration.setAllowCredentials(true); // Important for cookies/auth headers
        configuration.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all paths
        return source;
    }
}
