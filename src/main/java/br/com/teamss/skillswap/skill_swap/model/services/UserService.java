package br.com.teamss.skillswap.skill_swap.model.services;

import java.util.List;
import java.util.UUID;

import br.com.teamss.skillswap.skill_swap.model.entities.User;

public interface UserService {
    List<User> findAll(); // Assinatura original
    User findById(UUID id); // Assinatura original
    User save(User user); // Assinatura original
    User addSkills(UUID id, List<Long> skillIds); // Assinatura original
    User addRoles(UUID id, List<Long> roleIds); // Assinatura original
    User update(UUID id, User user); // Assinatura original
    void delete(UUID id); // Assinatura original

    // --- START: Accessibility Settings Specific Update ---
    /**
     * Atualiza apenas as configurações de acessibilidade para um determinado usuário.
     * @param userId O ID do usuário a ser atualizado.
     * @param librasAvatarEnabled O novo estado para a preferência do avatar LIBRAS.
     * @param preferredTheme O novo tema preferido (ex: "default", "high-contrast-dark").
     * @return A entidade User atualizada.
     * @throws jakarta.persistence.EntityNotFoundException se o usuário não for encontrado.
     */
    User updateAccessibilitySettings(UUID userId, boolean librasAvatarEnabled, String preferredTheme);
    // --- END: Accessibility Settings Specific Update ---
}

