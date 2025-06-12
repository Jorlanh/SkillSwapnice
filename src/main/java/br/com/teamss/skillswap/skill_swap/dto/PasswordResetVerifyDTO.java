package br.com.teamss.skillswap.skill_swap.dto;

// DTO para a verificação do código
public class PasswordResetVerifyDTO {
    private String email;
    private String code;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}