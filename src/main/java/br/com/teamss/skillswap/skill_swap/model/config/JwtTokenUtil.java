package br.com.teamss.skillswap.skill_swap.model.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // Import StringUtils

import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct; // Import PostConstruct
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    // Carrega o segredo a partir das propriedades da aplicação (application.properties)
    // que por sua vez referencia uma variável de ambiente (JWT_SECRET)
    @Value("${jwt.secret}")
    private String secretValue;

    // A chave secreta será inicializada após a injeção do valor
    private SecretKey secretKey;

    // O expiration pode continuar vindo do properties sem problema
    @Value("${jwt.expiration}")
    private Long expiration;

    // Método para inicializar a SecretKey após a injeção do valor 'secretValue'
    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(secretValue)) {
            throw new IllegalArgumentException("JWT secret key cannot be empty or null. Please set the JWT_SECRET environment variable.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretValue.getBytes());
    }

    // Gera um token JWT para o usuário
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    // Cria o token com base nos claims e no subject (username)
    private String createToken(Map<String, Object> claims, String subject) {
        // Usa a secretKey inicializada
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, Jwts.SIG.HS512) // Usa a secretKey
                .compact();
    }

    // Extrai o username do token
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Valida o token comparando o username e a data de expiração
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Extrai uma claim específica do token
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // Extrai todas as claims do token
    private Claims getAllClaimsFromToken(String token) {
        // Usa a secretKey inicializada
        return Jwts.parser()
                .verifyWith(secretKey) // Usa a secretKey
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Verifica se o token expirou
    private boolean isTokenExpired(String token) {
        final Date expirationDate = getClaimFromToken(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }
}