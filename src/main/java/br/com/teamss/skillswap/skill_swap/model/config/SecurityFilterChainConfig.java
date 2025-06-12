package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityFilterChainConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    public SecurityFilterChainConfig(UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(userDetailsService, jwtTokenUtil);
    }

    /**
     * 
     * @param http
     * @return
     * @throws Exception
     * 
     * 
     */
    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //     http
    //             .authorizeHttpRequests(auth -> auth
    //                     .requestMatchers("/api/chat/**", "/api/lesson/**", "/video-call").authenticated()
    //                     .requestMatchers(
    //                             "/api/home/posts/*/like",
    //                             "/api/home/posts/*/repost",
    //                             "/api/home/posts/*/comment",
    //                             "/api/home/posts",
    //                             "/api/home/communities/*/join",
    //                             "/api/home/notifications/**"
    //                     ).authenticated()
    //                     .requestMatchers(
    //                             "/api/register",
    //                             "/api/register/resend-code",
    //                             "/api/verify",
    //                             "/api/home/posts",
    //                             "/api/home/posts/*/share",
    //                             "/api/home/share/click",
    //                             "/api/home/communities",
    //                             "/api/home/trending-topics",
    //                             "/api/search/**"
    //                     ).permitAll()
    //                     .anyRequest().permitAll())
    //             .csrf(csrf -> csrf.disable())
    //             .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    //             .headers(headers -> headers
    //                     .frameOptions(frame -> frame.sameOrigin())
    //                     .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'self'"))
    //                     .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED))
    //                     .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
    //                     .contentTypeOptions(content -> content.disable()))
    //             .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
    //             .exceptionHandling(handling -> handling
    //                     .authenticationEntryPoint((request, response, authException) -> response.sendError(401, "Unauthorized"))
    //                     .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(403, "Forbidden")));

    //     return http.build();
    // }

     @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                // É importante desabilitar o CSRF para APIs e permitir o H2 Console
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**"))
                .disable()
            )
            .headers(headers -> headers
                // Permite que o H2 Console seja renderizado em um frame
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                // A REGRA DE OURO: PERMITE QUALQUER REQUISIÇÃO, FODA-SE
                .anyRequest().permitAll()
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Métodos específicos
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With")); // Cabeçalhos específicos
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}