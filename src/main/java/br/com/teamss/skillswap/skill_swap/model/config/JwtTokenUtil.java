package br.com.teamss.skillswap.skill_swap.model.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    private String secret = "z4B8g9pL2vF3kM7rJ5hG1wE0cT6yU4iNnQ9sV8uAFG8KK4LLASLD3KJDJ44J3fD7oP5xK6bZ1aC2eS5lH4j";

    // O expiration pode continuar vindo do properties sem problema
    @Value("${jwt.expiration}")
    private Long expiration;

    // Gera um token JWT para o usuário
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    // Cria o token com base nos claims e no subject (username)
    private String createToken(Map<String, Object> claims, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .claims(claims) // Método não depreciado
                .subject(subject) // Método não depreciado
                .issuedAt(new Date(System.currentTimeMillis())) // Método não depreciado
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Método não depreciado
                .signWith(key, Jwts.SIG.HS512) // Usando a nova enumeração de algoritmos
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
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
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