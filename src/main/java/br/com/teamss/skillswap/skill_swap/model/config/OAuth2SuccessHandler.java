package br.com.teamss.skillswap.skill_swap.model.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins; // Pega o http://localhost:8081 do seu properties

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        // Extrai o usuário do Auth0
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        
        // Aqui você pega o Token (ID Token ou Access Token) 
        // ou gera um novo JWT interno usando seu jwt.secret do properties
        String token = oidcUser.getIdToken().getTokenValue();

        // Define o alvo: a rota de callback que criamos no React
        // Usamos o primeiro origin da lista (localhost:8081)
        String targetUrl = allowedOrigins.split(",")[0] + "/oauth2/callback";

        // Injeta o token na URL de forma segura
        String redirectionUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .build().toUriString();

        // Limpa cookies de autenticação temporários (opcional) e redireciona
        getRedirectStrategy().sendRedirect(request, response, redirectionUrl);
    }
}