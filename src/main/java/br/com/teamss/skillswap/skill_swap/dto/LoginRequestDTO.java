package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDTO {

    @NotBlank(message = "Nome de usuário ou e-mail não pode ser vazio")
    private String usernameOrEmail;

    @NotBlank(message = "Senha não pode ser vazia")
    private String password;

    // Código 2FA é opcional no login inicial, mas pode ser necessário depois
    @Size(min = 6, max = 6, message = "Código 2FA deve ter 6 dígitos")
    private String twoFactorCode;

    // Método 2FA (EMAIL, SMS, GOOGLE_AUTH) - também opcional inicialmente
    private String twoFactorMethod;

    // Getters e Setters
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTwoFactorCode() { return twoFactorCode; }
    public void setTwoFactorCode(String twoFactorCode) { this.twoFactorCode = twoFactorCode; }
    public String getTwoFactorMethod() { return twoFactorMethod; }
    public void setTwoFactorMethod(String twoFactorMethod) { this.twoFactorMethod = twoFactorMethod; }
}
