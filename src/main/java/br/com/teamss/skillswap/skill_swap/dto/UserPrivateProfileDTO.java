package br.com.teamss.skillswap.skill_swap.dto;

import java.util.Set;
import java.util.UUID;

// DTO para o perfil PRIVADO de um usuário, visto apenas por ele mesmo. CONTÉM userId.
public record UserPrivateProfileDTO(
    UUID userId,
    String username,
    String email,
    String name,
    boolean verifiedBadge, // NOVO CAMPO
    ProfileDTO profile,
    Set<SkillDTO> skills,
    Set<String> roles
) {}