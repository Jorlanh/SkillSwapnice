package br.com.teamss.skillswap.skill_swap.filters;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.LocalUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BanCheckFilter extends OncePerRequestFilter {

    @Autowired
    private LocalUserService localUserService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Verifica se o usuário já foi autenticado pelo filtro OAuth 2.0
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt) {
            
            Jwt jwt = (Jwt) authentication.getPrincipal();
            
            try {
                // 2. Procura ou Cria o usuário local (Lógica JIT movida para o Service)
                User user = localUserService.findOrCreateUser(jwt);

                // 3. Verifica se o usuário local está banido
                if (user.isBanned()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado. Usuário banido.");
                    return; // Interrompe a requisição
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Erro ao provisionar usuário JIT: " + e.getMessage());
                // Decide se quer bloquear a requisição ou apenas logar
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erro ao processar token de usuário.");
                return;
            }
        }

        // 4. Continua a cadeia de filtros
        filterChain.doFilter(request, response);
    }
}