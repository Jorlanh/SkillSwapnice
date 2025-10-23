package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.LoginRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.config.JwtTokenUtil;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LoginService;
import br.com.teamss.skillswap.skill_swap.model.services.TwoFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException; // Import BadCredentialsException
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

     @Override
    public String authenticateAndGetToken(LoginRequestDTO loginRequest) {
        String usernameOrEmail = loginRequest.getUsernameOrEmail();
        String password = loginRequest.getPassword();

        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                // Lança BadCredentialsException se usuário não encontrado
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        // Lança BadCredentialsException se a senha não bater
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        if (isTwoFactorEnabled(user.getUserId())) {
             // Lança uma exceção específica para 2FA, que pode ser tratada no controller
             throw new TwoFactorAuthenticationRequiredException("Autenticação de dois fatores é necessária.");
        }

        // Se a autenticação for bem-sucedida e 2FA não for necessário, gere e retorne o token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return jwtTokenUtil.generateToken(userDetails);
    }

    @Override
    public boolean isTwoFactorEnabled(UUID userId) {
        return twoFactorService.isAnyMethodEnabled(userId);
    }

    @Override
    public void redirectToTwoFactor(UUID userId, HttpServletResponse response) {
        try {
            // Idealmente, o frontend lidaria com o redirecionamento ou exibição do formulário 2FA
            // Aqui, apenas sinalizamos a necessidade (poderia ser um status HTTP diferente ou corpo de resposta)
            // Lançar uma exceção pode ser mais apropriado para APIs REST
             throw new TwoFactorAuthenticationRequiredException("Redirecionando para 2FA para userId=" + userId);
             // Ou, se for uma aplicação web tradicional:
             // response.sendRedirect("/two-factor?userId=" + userId);
        } catch (/*IOException*/ Exception e) { // Captura a nova exceção também
            throw new RuntimeException("Erro ao indicar necessidade de 2FA", e);
        }
    }

    @Override
    public void redirectToHomepage(HttpServletResponse response) {
        try {
            // Em uma API REST, normalmente não redirecionamos, mas retornamos o token/sucesso.
            // Mantendo o redirecionamento caso seja uma aplicação web mista.
            response.sendRedirect("/homepage");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao redirecionar para a homepage", e);
        }
    }

    @Override
    public void redirectToRegister(HttpServletResponse response) {
        try {
            response.sendRedirect("/register");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao redirecionar para o registro", e);
        }
    }

    @Override
    public void loginWithGoogle(HttpServletRequest request, HttpServletResponse response) {
        // A lógica de login com Google geralmente envolve OAuth2 e pode ser mais complexa.
        // Simplificando aqui, assumindo que a autenticação externa já ocorreu.
        String googleEmail = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        if (googleEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(googleEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (isTwoFactorEnabled(user.getUserId())) {
                    redirectToTwoFactor(user.getUserId(), response);
                } else {
                    // Gerar token JWT e retornar em vez de redirecionar
                    final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                    String token = jwtTokenUtil.generateToken(userDetails);
                    // Idealmente, retornar o token na resposta JSON
                    System.out.println("Login Google bem-sucedido, token: " + token);
                    redirectToHomepage(response); // Mantendo redirecionamento por ora
                }
            } else {
                redirectToRegister(response);
            }
        } else {
            // Iniciar fluxo OAuth2
            try {
                response.sendRedirect("/oauth2/authorization/google");
            } catch (IOException e) {
                throw new RuntimeException("Erro ao iniciar login com Google", e);
            }
        }
    }

    @Override
    public boolean verifyTwoFactorCode(UUID userId, String method, String code) {
        // Delega a verificação para o TwoFactorService
        return twoFactorService.verifyCode(userId, method, code);
    }

    // Exceção customizada para sinalizar necessidade de 2FA
    public static class TwoFactorAuthenticationRequiredException extends RuntimeException {
        public TwoFactorAuthenticationRequiredException(String message) {
            super(message);
        }
    }
}