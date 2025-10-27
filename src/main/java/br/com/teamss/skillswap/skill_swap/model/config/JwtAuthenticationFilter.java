package br.com.teamss.skillswap.skill_swap.model.config;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.JwtBlocklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtBlocklistService jwtBlocklistService;
    private final UserRepository userRepository;

    @Autowired
    public JwtAuthenticationFilter(UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil,
                                   JwtBlocklistService jwtBlocklistService, UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtBlocklistService = jwtBlocklistService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (header != null && header.startsWith("Bearer ")) {
            jwt = header.substring(7);

            if (jwtBlocklistService.isTokenBlocked(jwt)) {
                logger.warn("Attempt to use a blocked JWT token.");
                chain.doFilter(request, response);
                return;
            }

            try {
                username = jwtTokenUtil.getUsernameFromToken(jwt);
            } catch (Exception e) {
                System.err.println("Erro ao processar JWT: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && user.isBanned()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Usuário banido.");
                return;
            }

            if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }
}