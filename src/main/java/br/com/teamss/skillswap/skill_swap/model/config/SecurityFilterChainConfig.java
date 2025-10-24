package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityFilterChainConfig {

    @Value("${cors.allowed-origins:http://localhost:4200,https://skillswap-frontend-tmub.onrender.com}")
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

    @Bean
    @Order(1)
    @Profile("dev")
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    // Rotas de Autenticação/Registo
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/login"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/register"),
                    AntPathRequestMatcher.antMatcher("/api/verify"),
                    AntPathRequestMatcher.antMatcher("/api/password-reset/**"),

                    // Rotas Públicas (GET)
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/skills/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/roles/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/search/**"),
                    // REGRAS REFINADAS PARA /api/home
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/posts"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/trending-topics"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/communities"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/trending-posts")
                    
                    // **CORREÇÃO DE SEGURANÇA: WebSockets não são mais públicos**
                    // AntPathRequestMatcher.antMatcher("/video-call/**"),
                    // AntPathRequestMatcher.antMatcher("/ws/**")
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Rate-Limit-Remaining"));
        configuration.setExposedHeaders(Arrays.asList("X-Rate-Limit-Remaining"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}