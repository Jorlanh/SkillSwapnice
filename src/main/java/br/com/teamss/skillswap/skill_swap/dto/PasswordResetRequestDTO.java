package br.com.teamss.skillswap.skill_swap.dto;

// DTO para a solicitação de envio do código
public class PasswordResetRequestDTO {
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}