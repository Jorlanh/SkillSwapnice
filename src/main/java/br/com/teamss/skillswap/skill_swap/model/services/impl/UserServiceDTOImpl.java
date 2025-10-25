package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.*;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceDTOImpl implements UserServiceDTO {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDTO toUserDTO(User user) {
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

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<SkillDTO> skills = user.getSkills().stream()
                .map(skill -> new SkillDTO(
                    skill.getSkillId(),
                    skill.getName(),
                    skill.getDescription(),
                    skill.getCategory(),
                    skill.getLevel()
                ))
                .collect(Collectors.toSet());

        UserDTO userDTO = new UserDTO(
                user.getUserId(),
                user.getUsername(),
                roles,
                profileDTO,
                skills);
        
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setTwoFactorSecret(user.getTwoFactorSecret());
        userDTO.setVerificationCode(user.getVerificationCode());

        return userDTO;
    }

    @Override
    public List<UserSummaryDTO> findAllSummaries() {
        return userRepository.findAll().stream()
                .map(user -> new UserSummaryDTO(user.getUsername(), user.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO findByIdDTO(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        return toUserDTO(user);
    }
    
    @Override
    public UserSummaryDTO findSummaryByIdDTO(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        return new UserSummaryDTO(user.getUsername(), user.getName());
    }

    @Override
    public void updateVerificationCode(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        user.setVerificationCode(code);
        userRepository.save(user);
    }

    @Override
    public void updateVerificationStatus(UUID userId, boolean verified) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        user.setVerified(verified);
        if (verified) {
            user.setVerifiedAt(Instant.now());
        } else {
            user.setVerifiedAt(null);
        }
        userRepository.save(user);
    }

    @Override
    public void saveUserDTO(UserDTO userDTO) {
        User user = userRepository.findById(userDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setTwoFactorSecret(userDTO.getTwoFactorSecret());
        user.setVerificationCode(userDTO.getVerificationCode());
        userRepository.save(user);
    }

    @Override
    public UserDTO findByUsernameDTO(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com username: " + username));
        return toUserDTO(user);
    }
    
    @Override
    public UserDTO getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador autenticado não encontrado na base de dados."));
        return toUserDTO(user);
    }

    // NOVAS IMPLEMENTAÇÕES
    @Override
    public UserPublicProfileDTO toUserPublicProfileDTO(User user) {
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

        Set<SkillDTO> skills = user.getSkills().stream()
            .map(skill -> new SkillDTO(skill.getSkillId(), skill.getName(), skill.getDescription(), skill.getCategory(), skill.getLevel()))
            .collect(Collectors.toSet());

        return new UserPublicProfileDTO(user.getUsername(), user.getName(), profileDTO, skills);
    }

    @Override
    public UserPrivateProfileDTO toUserPrivateProfileDTO(User user) {
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

        Set<SkillDTO> skills = user.getSkills().stream()
            .map(skill -> new SkillDTO(skill.getSkillId(), skill.getName(), skill.getDescription(), skill.getCategory(), skill.getLevel()))
            .collect(Collectors.toSet());

        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        return new UserPrivateProfileDTO(
            user.getUserId(), user.getUsername(), user.getEmail(), user.getName(),
            profileDTO, skills, roles
        );
    }

    @Override
    public UserPublicProfileDTO findPublicProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com username: " + username));
        return toUserPublicProfileDTO(user);
    }
}