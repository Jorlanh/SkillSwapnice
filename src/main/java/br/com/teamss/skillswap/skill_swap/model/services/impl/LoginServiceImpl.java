package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.LoginRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.config.JwtTokenUtil;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LoginService;
import br.com.teamss.skillswap.skill_swap.model.services.TwoFactorService;
import org.springframework.beans.factory.annotation.Autowired;
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
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (passwordEncoder.matches(password, user.getPassword())) {
            if (isTwoFactorEnabled(user.getUserId())) {
                 throw new RuntimeException("Autenticação de dois fatores é necessária.");
            }
            // Se a autenticação for bem-sucedida, gere e retorne o token
            final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            return jwtTokenUtil.generateToken(userDetails);
        }

        throw new RuntimeException("Credenciais inválidas");
    }

    @Override
    public boolean isTwoFactorEnabled(UUID userId) {
        return twoFactorService.isAnyMethodEnabled(userId);
    }

    @Override
    public void redirectToTwoFactor(UUID userId, HttpServletResponse response) {
        try {
            response.sendRedirect("/two-factor?userId=" + userId);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao redirecionar para 2FA", e);
        }
    }

    @Override
    public void redirectToHomepage(HttpServletResponse response) {
        try {
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
        String googleEmail = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        if (googleEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(googleEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (isTwoFactorEnabled(user.getUserId())) {
                    redirectToTwoFactor(user.getUserId(), response);
                } else {
                    redirectToHomepage(response);
                }
            } else {
                redirectToRegister(response);
            }
        } else {
            try {
                response.sendRedirect("/oauth2/authorization/google");
            } catch (IOException e) {
                throw new RuntimeException("Erro ao iniciar login com Google", e);
            }
        }
    }

    @Override
    public boolean verifyTwoFactorCode(UUID userId, String method, String code) {
        return twoFactorService.verifyCode(userId, method, code);
    }
}