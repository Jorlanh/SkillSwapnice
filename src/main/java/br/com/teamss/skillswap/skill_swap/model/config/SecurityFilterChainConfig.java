package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; // Import Profile
import org.springframework.core.annotation.Order; // Import Order
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
import static org.springframework.security.config.Customizer.withDefaults; // Import withDefaults

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
    @Order(1) // Garante que esta configuração seja aplicada primeiro
    @Profile("dev") // Aplica esta configuração APENAS quando o perfil 'dev' está ativo
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(AntPathRequestMatcher.antMatcher("/h2-console/**")) // Aplica APENAS para /h2-console/**
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // Permite tudo dentro do H2 Console
            .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**"))) // Desabilita CSRF para o H2 Console
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // Permite iframes para o H2 Console
        return http.build();
    }


    @Bean
    @Order(2) // Esta configuração será aplicada depois da do H2 Console
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // ATIVA A CONFIGURAÇÃO DE CORS QUE VOCÊ DEFINIU ABAIXO
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Desabilita CSRF, pois a aplicação é stateless e usa tokens JWT
            .csrf(AbstractHttpConfigurer::disable)

            // Configura a política de sessão para ser stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configura as regras de autorização para cada endpoint
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (não precisam de autenticação)
                .requestMatchers(
                    // AntPathRequestMatcher.antMatcher("/h2-console/**"), // Removido daqui, tratado pelo perfil 'dev'
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/login"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/register"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/verify"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/password-reset/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/skills/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/roles/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/search/**"),
                    AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/home/**"),
                    // Endpoints WebSocket geralmente precisam de tratamento especial, mas permitir por agora
                    AntPathRequestMatcher.antMatcher("/video-call/**"),
                    AntPathRequestMatcher.antMatcher("/ws/**") // Exemplo genérico para WebSocket
                ).permitAll()

                // Qualquer outra requisição precisa de autenticação
                .anyRequest().authenticated()
            )

            // Adiciona o filtro de autenticação JWT antes do filtro padrão de usuário/senha
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
            // .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // Removido daqui, tratado pelo perfil 'dev' para H2

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
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Rate-Limit-Remaining")); // Adicionado X-Rate-Limit-Remaining
        configuration.setExposedHeaders(Arrays.asList("X-Rate-Limit-Remaining")); // Expor o header de Rate Limit
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}