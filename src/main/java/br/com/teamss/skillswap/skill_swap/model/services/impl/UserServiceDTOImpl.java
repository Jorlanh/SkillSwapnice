package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.*;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill; // Importar Skill
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserService; // Importar UserService
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // Importar Authentication
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceDTOImpl implements UserServiceDTO {

    // Injeção de dependências via construtor é preferível
    private final UserRepository userRepository;
    private final UserService userService; // Injete a interface UserService

    @Autowired // Mantido Autowired como no original
    public UserServiceDTOImpl(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService; // Armazene a instância injetada
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null; // Retornar null se o usuário for null
        }

        ProfileDTO profileDTO = null;
        if (user.getProfile() != null) {
            Profile profile = user.getProfile();
            profileDTO = new ProfileDTO(
                    profile.getProfileId(),
                    profile.getDescription(),
                    profile.getImageUrl(),
                    profile.getLocation(),
                    profile.getContactInfo(),
                    profile.getSocialMediaLinks(),
                    profile.getAvailabilityStatus(),
                    profile.getInterests(),
                    profile.getExperienceLevel(),
                    profile.getEducationLevel());
        }

        Set<String> roles = (user.getRoles() != null) ? user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()) : Collections.emptySet(); // Usar Collections.emptySet() para nulo

        Set<SkillDTO> skills = (user.getSkills() != null) ? user.getSkills().stream()
                .map(skill -> new SkillDTO(
                        skill.getSkillId(),
                        skill.getName(),
                        skill.getDescription(),
                        skill.getCategory(),
                        skill.getLevel()
                ))
                .collect(Collectors.toSet()) : Collections.emptySet(); // Usar Collections.emptySet() para nulo

        UserDTO userDTO = new UserDTO(); // Use construtor padrão e setters
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRoles(roles);
        userDTO.setProfile(profileDTO);
        userDTO.setSkills(skills);
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setTwoFactorSecret(user.getTwoFactorSecret());
        userDTO.setVerificationCode(user.getVerificationCode()); // Manter se necessário

        // --- START: Accessibility Settings ---
        userDTO.setLibrasAvatarEnabled(user.isLibrasAvatarEnabled());
        userDTO.setPreferredTheme(user.getPreferredTheme());
        // --- END: Accessibility Settings ---

        return userDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> findAllSummaries() {
        return userRepository.findAll().stream()
                .map(user -> new UserSummaryDTO(user.getUsername(), user.getName(), user.isVerifiedBadge()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findByIdDTO(UUID id) {
        // Delegar busca para UserService para reutilizar a lógica de EntityNotFoundException
        User user = userService.findById(id);
        return toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDTO findSummaryByIdDTO(UUID id) {
        // Delegar busca para UserService
        User user = userService.findById(id);
        return new UserSummaryDTO(user.getUsername(), user.getName(), user.isVerifiedBadge());
    }

    @Override
    @Transactional
    public void updateVerificationCode(UUID userId, String code) {
        // Delegar busca para UserService
        User user = userService.findById(userId);
        user.setVerificationCode(code);
        // Opcional: definir expiração aqui também
        userRepository.save(user); // Necessário salvar após modificação
    }

    @Override
    @Transactional
    public void updateVerificationStatus(UUID userId, boolean verified) {
        // Delegar busca para UserService
        User user = userService.findById(userId);
        user.setVerified(verified);
        user.setVerifiedAt(verified ? Instant.now() : null);
        if (verified) { // Limpar código e expiração ao verificar
            user.setVerificationCode(null);
            user.setVerificationCodeExpiry(null);
        }
        userRepository.save(user); // Necessário salvar após modificação
    }

    /**
     * @deprecated Use métodos de atualização mais específicos como updateAccessibilitySettingsDTO.
     * Este método pode sobrescrever campos não intencionalmente.
     */
    @Override
    @Deprecated
    @Transactional
    public void saveUserDTO(UserDTO userDTO) {
        // Delegar busca para UserService
        User user = userService.findById(userDTO.getUserId());

        // Atualização parcial - PERIGOSO se o DTO não estiver completo
        if(userDTO.getUsername() != null) user.setUsername(userDTO.getUsername());
        if(userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        // user.setName(userDTO.getName()); // Nome não está no DTO original
        if(userDTO.getPhoneNumber() != null) user.setPhoneNumber(userDTO.getPhoneNumber());
        if(userDTO.getTwoFactorSecret() != null) user.setTwoFactorSecret(userDTO.getTwoFactorSecret());
        if(userDTO.getVerificationCode() != null) user.setVerificationCode(userDTO.getVerificationCode());

        // Atualizar acessibilidade
        user.setLibrasAvatarEnabled(userDTO.isLibrasAvatarEnabled()); // Boolean atualiza sempre
        if(userDTO.getPreferredTheme() != null) user.setPreferredTheme(userDTO.getPreferredTheme());

        // NÃO atualizar roles, skills, profile aqui - usar métodos específicos em UserService
        userRepository.save(user); // Necessário salvar após modificação
    }


    @Override
    @Transactional(readOnly = true)
    public UserDTO findByUsernameDTO(String username) {
        User user = userRepository.findByUsername(username) // Busca direta pelo repositório é eficiente aqui
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com username: " + username));
        return toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal())) {
             throw new IllegalStateException("Nenhum usuário autenticado encontrado no contexto de segurança.");
         }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        // Buscar usuário pelo username garantido pela autenticação
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado ('" + username + "') não encontrado na base de dados."));
        return toUserDTO(user);
    }

    // NOVAS IMPLEMENTAÇÕES originais mantidas
    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileDTO toUserPublicProfileDTO(User user) {
        if (user == null) return null;
        Profile profile = user.getProfile();
        ProfileDTO profileDTO = null;
        if (profile != null) {
            profileDTO = new ProfileDTO(
                profile.getProfileId(), profile.getDescription(), profile.getImageUrl(),
                profile.getLocation(), profile.getContactInfo(), profile.getSocialMediaLinks(),
                profile.getAvailabilityStatus(), profile.getInterests(),
                profile.getExperienceLevel(), profile.getEducationLevel()
            );
        }

        Set<SkillDTO> skills = (user.getSkills() != null) ? user.getSkills().stream()
            .map(skill -> new SkillDTO(skill.getSkillId(), skill.getName(), skill.getDescription(), skill.getCategory(), skill.getLevel()))
            .collect(Collectors.toSet()) : Collections.emptySet();

        return new UserPublicProfileDTO(user.getUsername(), user.getName(), user.isVerifiedBadge(), profileDTO, skills);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPrivateProfileDTO toUserPrivateProfileDTO(User user) {
        if (user == null) return null;
        Profile profile = user.getProfile();
        ProfileDTO profileDTO = null;
        if (profile != null) {
            profileDTO = new ProfileDTO(
                profile.getProfileId(), profile.getDescription(), profile.getImageUrl(),
                profile.getLocation(), profile.getContactInfo(), profile.getSocialMediaLinks(),
                profile.getAvailabilityStatus(), profile.getInterests(),
                profile.getExperienceLevel(), profile.getEducationLevel()
            );
        }

        Set<SkillDTO> skills = (user.getSkills() != null) ? user.getSkills().stream()
            .map(skill -> new SkillDTO(skill.getSkillId(), skill.getName(), skill.getDescription(), skill.getCategory(), skill.getLevel()))
            .collect(Collectors.toSet()) : Collections.emptySet();

        Set<String> roles = (user.getRoles() != null) ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) : Collections.emptySet();

        return new UserPrivateProfileDTO(
            user.getUserId(), user.getUsername(), user.getEmail(), user.getName(),
            user.isVerifiedBadge(), profileDTO, skills, roles,
            // --- START: Accessibility Settings ---
            user.isLibrasAvatarEnabled(),
            user.getPreferredTheme()
            // --- END: Accessibility Settings ---
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileDTO findPublicProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com username: " + username));
        return toUserPublicProfileDTO(user);
    }

    // --- START: Accessibility Settings Specific Update DTO Method Implementation ---
    @Override
    @Transactional // Operação de escrita
    public UserDTO updateAccessibilitySettingsDTO(UUID userId, boolean librasAvatarEnabled, String preferredTheme) {
        // Delega a lógica de atualização para o UserService
        User updatedUser = userService.updateAccessibilitySettings(userId, librasAvatarEnabled, preferredTheme);
        // Converte a entidade atualizada de volta para DTO
        return toUserDTO(updatedUser);
    }
    // --- END: Accessibility Settings Specific Update DTO Method Implementation ---
}