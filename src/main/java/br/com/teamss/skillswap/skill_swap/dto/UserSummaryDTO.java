package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

// DTO para exibir apenas informações públicas do usuário
public class UserSummaryDTO {

    private UUID userId;
    private String username;
    private String name;

    // Construtor, Getters e Setters
    public UserSummaryDTO(UUID userId, String username, String name) {
        this.userId = userId;
        this.username = username;
        this.name = name;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}