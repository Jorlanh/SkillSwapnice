package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_two_factor_configs")
public class TwoFactorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "secret")
    private String secret;

    @Column(name = "created_at")
    private Instant  createdAt;

    @Column(name = "updated_at")
    private Instant  updatedAt;

    // Construtores
    public TwoFactorConfig() {}

    public TwoFactorConfig(UUID userId, String method, boolean enabled, String secret) {
        this.userId = userId;
        this.method = method;
        this.enabled = enabled;
        this.secret = secret;
    }

    // Getters e Setters
    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Instant  getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant  createdAt) { this.createdAt = createdAt; }
    public Instant  getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant  updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}