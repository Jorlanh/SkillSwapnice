package br.com.teamss.skillswap.skill_swap.filters;

import br.com.teamss.skillswap.skill_swap.model.services.AccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    @Autowired
    private AccessLogService accessLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            // Assume que o principal contém o userId (ajustar conforme sua implementação de autenticação)
            String userId = authentication.getName(); // Ou extraia o userId de outra forma
            try {
                UUID uuid = UUID.fromString(userId);
                accessLogService.logAccess(uuid, request);
            } catch (IllegalArgumentException e) {
                // Log de erro, se necessário
            }
        }
        filterChain.doFilter(request, response);
    }
}