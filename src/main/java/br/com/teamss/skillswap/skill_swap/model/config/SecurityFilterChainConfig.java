package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import java.util.List;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desabilita CSRF, pois a aplicação é stateless e usa tokens JWT
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configura a política de sessão para ser stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configura as regras de autorização para cada endpoint
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (não precisam de autenticação)
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/h2-console/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/login"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/register"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/verify"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/password-reset/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/skills/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/roles/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/search/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/**")
                ).permitAll()

                // Endpoint para WebSocket (requer uma configuração especial, mas vamos permitir por enquanto)
                .requestMatchers(AntPathRequestMatcher.antMatcher("/video-call/**")).permitAll()

                // Qualquer outra requisição precisa de autenticação
                .anyRequest().authenticated()
            )
            
            // Adiciona o filtro de autenticação JWT antes do filtro padrão de usuário/senha
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // Permite que o console H2 seja renderizado em um frame
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

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
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}