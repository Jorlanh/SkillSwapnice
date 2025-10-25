package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

public class UserSummaryDTO {

    private String username;
    private String name;

    public UserSummaryDTO(String username, String name) {
        this.username = username;
        this.name = name;
    }
    
    // Construtor antigo mantido para retrocompatibilidade interna, se necessário, mas não para exposição.
    public UserSummaryDTO(UUID userId, String username, String name) {
        this.username = username;
        this.name = name;
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