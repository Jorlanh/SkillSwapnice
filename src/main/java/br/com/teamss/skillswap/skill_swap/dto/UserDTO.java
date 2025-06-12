package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotNull; // ADICIONADO
import java.util.Set;
import java.util.UUID;

public class UserDTO {
    @NotNull(message = "userId não pode ser nulo") // MOVIDO PARA O CAMPO
    private UUID userId;
    private String username;
    private Set<String> roles;
    private ProfileDTO profile;
    private Set<SkillDTO> skills;
    private String twoFactorSecret;
    private String phoneNumber;
    private String email;
    private String verificationCode;

    public UserDTO(UUID userId, String username, Set<String> roles, ProfileDTO profile, Set<SkillDTO> skills) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.profile = profile;
        this.skills = skills;
    }

    // Getters e Setters existentes
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public ProfileDTO getProfile() { return profile; }
    public void setProfile(ProfileDTO profile) { this.profile = profile; }
    public Set<SkillDTO> getSkills() { return skills; }
    public void setSkills(Set<SkillDTO> skills) { this.skills = skills; }
    public String getTwoFactorSecret() { return twoFactorSecret; }
    public void setTwoFactorSecret(String twoFactorSecret) { this.twoFactorSecret = twoFactorSecret; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
}