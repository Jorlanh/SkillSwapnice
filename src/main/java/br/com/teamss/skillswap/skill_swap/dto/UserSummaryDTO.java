package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

// DTO para exibir APENAS informações públicas e não sensíveis de um usuário.
public class UserSummaryDTO {

    // O userId foi REMOVIDO dos campos para exposição pública.
    private String username;
    private String name;

    // Construtor ajustado para não exigir mais o userId.
    public UserSummaryDTO(String username, String name) {
        this.username = username;
        this.name = name;
    }
    
    // Construtor antigo mantido para retrocompatibilidade, caso necessário em outros pontos.
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