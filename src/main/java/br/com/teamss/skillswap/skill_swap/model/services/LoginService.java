package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.LoginRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public interface LoginService {
    String authenticateAndGetToken(LoginRequestDTO loginRequest);
    boolean isTwoFactorEnabled(UUID userId);
    void redirectToTwoFactor(UUID userId, HttpServletResponse response);
    void redirectToHomepage(HttpServletResponse response);
    void redirectToRegister(HttpServletResponse response);
    void loginWithGoogle(HttpServletRequest request, HttpServletResponse response);
    boolean verifyTwoFactorCode(UUID userId, String method, String code); // Novo método
}