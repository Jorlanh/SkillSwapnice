package br.com.teamss.skillswap.skill_swap.model.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

@Entity
@Table(name = "tb_users")
public class User {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "birth_date")
    private Date birthDate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expiry")
    private Instant verificationCodeExpiry;

    @Column(name = "verified")
    private Boolean verified = false; // Valor padrão explícito

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now(); // Valor padrão explícito

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "verified_badge")
    private boolean verifiedBadge = false;

    @Column(name = "banned")
    private boolean banned = false;

    // --- START: Accessibility Settings ---
    @Column(name = "libras_avatar_enabled", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean librasAvatarEnabled = false;

    @Column(name = "preferred_theme", length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'default'")
    private String preferredTheme = "default"; // e.g., "default", "high-contrast-dark", "high-contrast-light"
    // --- END: Accessibility Settings ---

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // FetchType LAZY é geralmente melhor para OneToOne
    @JsonManagedReference
    private Profile profile;

    @ManyToMany(fetch = FetchType.EAGER) // Manter EAGER se roles são frequentemente necessários com o usuário
    @JoinTable(name = "tb_user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>(); // Inicializar coleções

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tb_user_skills", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new HashSet<>(); // Inicializar coleções

    @ElementCollection(fetch = FetchType.LAZY) // Usar LAZY para coleções
    @CollectionTable(name = "tb_user_following", joinColumns = @JoinColumn(name = "user_id")) // Nome de tabela mais claro
    @Column(name = "following_id")
    private List<UUID> followingIds = new ArrayList<>(); // Inicializar coleções

    @ElementCollection(fetch = FetchType.LAZY) // Usar LAZY para coleções
    @CollectionTable(name = "tb_user_communities", joinColumns = @JoinColumn(name = "user_id")) // Nome de tabela mais claro
    @Column(name = "community_id")
    private List<UUID> communityIds = new ArrayList<>(); // Inicializar coleções

    @Column(name = "bio", length = 500) // Limitar tamanho do bio
    private String bio;

    @Column(name = "country", length = 100) // Limitar tamanho
    private String country;

    @Column(name = "city", length = 100) // Limitar tamanho
    private String city;

    @Column(name = "state", length = 100) // Limitar tamanho
    private String state;

    @Column(name = "followers", columnDefinition = "INT DEFAULT 0") // Valor padrão explícito
    private int followers = 0;

    @ElementCollection(fetch = FetchType.LAZY) // Usar LAZY para coleções
    @CollectionTable(name = "tb_user_messages", joinColumns = @JoinColumn(name = "user_id")) // Nome de tabela mais claro
    @Column(name = "message_id")
    private List<UUID> messageIds = new ArrayList<>(); // Inicializar coleções

    public User() {
    }

    // Getters e Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Instant getVerificationCodeExpiry() {
        return verificationCodeExpiry;
    }

    public void setVerificationCodeExpiry(Instant verificationCodeExpiry) {
        this.verificationCodeExpiry = verificationCodeExpiry;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = (roles != null) ? roles : new HashSet<>(); // Garantir inicialização
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void setSkills(Set<Skill> skills) {
        this.skills = (skills != null) ? skills : new HashSet<>(); // Garantir inicialização
    }

    public List<UUID> getFollowingIds() {
        return followingIds;
    }

    public void setFollowingIds(List<UUID> followingIds) {
        this.followingIds = (followingIds != null) ? followingIds : new ArrayList<>(); // Garantir inicialização
    }

    public List<UUID> getCommunityIds() {
        return communityIds;
    }

    public void setCommunityIds(List<UUID> communityIds) {
        this.communityIds = (communityIds != null) ? communityIds : new ArrayList<>(); // Garantir inicialização
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public List<UUID> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<UUID> messageIds) {
        this.messageIds = (messageIds != null) ? messageIds : new ArrayList<>(); // Garantir inicialização
    }

    public boolean isVerifiedBadge() {
        return verifiedBadge;
    }

    public void setVerifiedBadge(boolean verifiedBadge) {
        this.verifiedBadge = verifiedBadge;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    // --- START: Accessibility Settings Getters/Setters ---
    public boolean isLibrasAvatarEnabled() {
        return librasAvatarEnabled;
    }

    public void setLibrasAvatarEnabled(boolean librasAvatarEnabled) {
        this.librasAvatarEnabled = librasAvatarEnabled;
    }

    public String getPreferredTheme() {
        return preferredTheme;
    }

    public void setPreferredTheme(String preferredTheme) {
        this.preferredTheme = preferredTheme;
    }
    // --- END: Accessibility Settings Getters/Setters ---

    // --- equals() and hashCode() based on userId (Opcional, mas recomendado) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId != null && userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}