package br.com.teamss.skillswap.skill_swap.model.services;

public interface PasswordResetService {
    void requestPasswordReset(String email); // Envia o código para o e-mail
    boolean verifyResetCode(String email, String code); // Verifica o código inserido
    void resetPassword(String email, String newPassword, String confirmPassword); // Altera a senha
}