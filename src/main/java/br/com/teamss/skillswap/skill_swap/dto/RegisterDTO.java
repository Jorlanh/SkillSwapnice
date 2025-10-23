package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; // Adicionado para birthDate
import jakarta.validation.constraints.Past; // Adicionado para birthDate
import jakarta.validation.constraints.Size; // Adicionado para password e username
import java.time.LocalDate;

public class RegisterDTO {
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "O e-mail deve ser válido")
    @Size(max = 255, message = "E-mail não pode exceder 255 caracteres")
    private String email;

    @NotBlank(message = "O nome de usuário é obrigatório")
    @Size(min = 3, max = 50, message = "Nome de usuário deve ter entre 3 e 50 caracteres")
    // Adicionar validação de caracteres permitidos se necessário (ex: @Pattern)
    private String username;

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate birthDate;

    @NotBlank(message = "O número de celular é obrigatório")
    // Adicionar validação de formato de telefone se necessário (ex: @Pattern)
    @Size(min = 10, max = 20, message = "Número de telefone inválido")
    private String phoneNumber;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    // Considerar adicionar requisitos de complexidade (ex: @Pattern)
    private String password;

    @NotBlank(message = "A confirmação de senha é obrigatória")
    private String confirmPassword; // A validação de igualdade é feita no controller/service

    // Getters e Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
