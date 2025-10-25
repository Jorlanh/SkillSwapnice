package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    public JwtHandshakeInterceptor(JwtTokenUtil jwtTokenUtil, UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        // Corrigido: fromUri() em vez de fromHttpRequest()
        String token = UriComponentsBuilder.fromUri(request.getURI())
                                           .build()
                                           .getQueryParams()
                                           .getFirst("token");

        if (token == null || token.trim().isEmpty()) {
            return false; // Rejeita a conexão se não houver token
        }

        try {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtTokenUtil.validateToken(token, userDetails)) {
                    // Adiciona o principal do usuário aos atributos da sessão WebSocket
                    attributes.put("user", userDetails);
                    return true; // Permite o handshake
                }
            }
        } catch (Exception e) {
            // Log de erro (opcional)
            return false; // Rejeita em caso de token inválido ou expirado
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
        // Nenhuma ação necessária após o handshake
    }
}
