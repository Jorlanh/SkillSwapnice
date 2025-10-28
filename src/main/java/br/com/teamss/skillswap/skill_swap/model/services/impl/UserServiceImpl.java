package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.Profile; // Importar Profile
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.RoleRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importar Transactional
import org.springframework.util.StringUtils; // Importar StringUtils

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    // Injeção de dependências via construtor é preferível
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SkillRepository skillRepository;
    private final ProfileRepository profileRepository;

    @Autowired // Mantido Autowired como no original, mas construtor seria melhor
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, SkillRepository skillRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.skillRepository = skillRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional(readOnly = true) // Marcar como read-only
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true) // Marcar como read-only
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + id));
    }

    @Override
    @Transactional // Operação de escrita
    public User save(User user) {
        // Garantir ID se for novo usuário
        if (user.getUserId() == null) {
            user.setUserId(UUID.randomUUID());
        }

        // Lógica original para roles e skills
        user.setRoles(manageRoles(user.getRoles()));
        user.setSkills(manageSkills(user.getSkills()));

        // Lógica original para profile
        if (user.getProfile() != null) {
             if (user.getProfile().getProfileId() != null) {
                 // Perfil existente, buscar e associar (se necessário, verificar se já está associado corretamente)
                 var managedProfile = profileRepository.findById(user.getProfile().getProfileId())
                         .orElseThrow(() -> new EntityNotFoundException(
                                 "Profile não encontrado: " + user.getProfile().getProfileId()));
                 // Garantir que a associação bidirecional esteja correta
                 managedProfile.setUser(user);
                 user.setProfile(managedProfile);
             } else {
                 // Novo perfil, garantir associação bidirecional antes de salvar (cascade cuidará da persistência)
                 user.getProfile().setUser(user);
                 // O save do User via cascade deve persistir o novo Profile
             }
        } else {
             // Se nenhum perfil for fornecido, criar um perfil padrão
             Profile defaultProfile = new Profile();
             defaultProfile.setUser(user);
             user.setProfile(defaultProfile); // Cascade deve salvar o novo profile
        }


        return userRepository.save(user);
    }

     // Helper methods (privados) para buscar roles e skills gerenciados
     private Set<Role> manageRoles(Set<Role> requestedRoles) {
         if (requestedRoles == null || requestedRoles.isEmpty()) {
             return new HashSet<>();
         }
         return requestedRoles.stream()
             .map(role -> role.getRoleId() != null ? roleRepository.findById(role.getRoleId())
                     .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + role.getRoleId())) : role) // Busca se ID existe
             .collect(Collectors.toSet());
     }

     private Set<Skill> manageSkills(Set<Skill> requestedSkills) {
         if (requestedSkills == null || requestedSkills.isEmpty()) {
             return new HashSet<>();
         }
         return requestedSkills.stream()
             .map(skill -> skill.getSkillId() != null ? skillRepository.findById(skill.getSkillId())
                     .orElseThrow(() -> new EntityNotFoundException("Skill não encontrada: " + skill.getSkillId())) : skill) // Busca se ID existe
             .collect(Collectors.toSet());
     }


    @Override
    @Transactional // Operação de escrita
    public User addSkills(UUID userId, List<Long> skillIds) {
        User user = findById(userId); // Reutilizar findById

        if (skillIds != null && !skillIds.isEmpty()) { // Verificar se a lista não é nula ou vazia
            Set<Skill> skillsToAdd = skillIds.stream()
                    .map(id -> skillRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Skill não encontrada: " + id)))
                    .collect(Collectors.toSet());
            user.getSkills().addAll(skillsToAdd); // Adiciona às skills existentes
        }
        // Save pode ser redundante se a entidade estiver gerenciada pela transação, mas explícito é seguro
        return userRepository.save(user);
    }

    @Override
    @Transactional // Operação de escrita
    public User addRoles(UUID userId, List<Long> roleIds) {
        User user = findById(userId); // Reutilizar findById

         if (roleIds != null && !roleIds.isEmpty()) { // Verificar se a lista não é nula ou vazia
            Set<Role> rolesToAdd = roleIds.stream()
                    .map(id -> roleRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + id)))
                    .collect(Collectors.toSet());
            user.getRoles().addAll(rolesToAdd); // Adiciona às roles existentes
        }
        // Save pode ser redundante
        return userRepository.save(user);
    }

    @Override
    @Transactional // Operação de escrita
    public User update(UUID id, User userUpdates) {
        User existingUser = findById(id); // Reutilizar findById para garantir que existe

        // Atualizar campos básicos apenas se fornecidos
        if (StringUtils.hasText(userUpdates.getUsername())) existingUser.setUsername(userUpdates.getUsername());
        // Não atualizar senha aqui - deve ser um processo separado e seguro
        // if (StringUtils.hasText(userUpdates.getPassword())) existingUser.setPassword(userUpdates.getPassword()); // REMOVIDO - inseguro
        if (StringUtils.hasText(userUpdates.getName())) existingUser.setName(userUpdates.getName());
        if (StringUtils.hasText(userUpdates.getEmail())) existingUser.setEmail(userUpdates.getEmail());
        if (userUpdates.getBirthDate() != null) existingUser.setBirthDate(userUpdates.getBirthDate());
        if (StringUtils.hasText(userUpdates.getPhoneNumber())) existingUser.setPhoneNumber(userUpdates.getPhoneNumber());
        if (StringUtils.hasText(userUpdates.getBio())) existingUser.setBio(userUpdates.getBio());
        if (StringUtils.hasText(userUpdates.getCountry())) existingUser.setCountry(userUpdates.getCountry());
        if (StringUtils.hasText(userUpdates.getCity())) existingUser.setCity(userUpdates.getCity());
        if (StringUtils.hasText(userUpdates.getState())) existingUser.setState(userUpdates.getState());
        // Campos como verified, verifiedAt, createdAt geralmente não são atualizados aqui

        // Atualizar Roles (substituindo as existentes) se fornecido
        if (userUpdates.getRoles() != null) {
            existingUser.setRoles(manageRoles(userUpdates.getRoles()));
        }

        // Atualizar Skills (substituindo as existentes) se fornecido
        if (userUpdates.getSkills() != null) {
            existingUser.setSkills(manageSkills(userUpdates.getSkills()));
        }

        // Atualizar Profile se fornecido
        if (userUpdates.getProfile() != null) {
             Profile existingProfile = existingUser.getProfile();
             if (existingProfile == null) { // Criar se não existir
                 existingProfile = new Profile();
                 existingProfile.setUser(existingUser);
                 existingUser.setProfile(existingProfile);
             }
             Profile profileUpdates = userUpdates.getProfile();
             // Atualizar campos do perfil se fornecidos
             if (StringUtils.hasText(profileUpdates.getDescription())) existingProfile.setDescription(profileUpdates.getDescription());
             if (StringUtils.hasText(profileUpdates.getImageUrl())) existingProfile.setImageUrl(profileUpdates.getImageUrl());
             if (StringUtils.hasText(profileUpdates.getLocation())) existingProfile.setLocation(profileUpdates.getLocation());
             if (StringUtils.hasText(profileUpdates.getContactInfo())) existingProfile.setContactInfo(profileUpdates.getContactInfo());
             if (StringUtils.hasText(profileUpdates.getSocialMediaLinks())) existingProfile.setSocialMediaLinks(profileUpdates.getSocialMediaLinks());
             if (StringUtils.hasText(profileUpdates.getAvailabilityStatus())) existingProfile.setAvailabilityStatus(profileUpdates.getAvailabilityStatus());
             if (StringUtils.hasText(profileUpdates.getInterests())) existingProfile.setInterests(profileUpdates.getInterests());
             if (StringUtils.hasText(profileUpdates.getExperienceLevel())) existingProfile.setExperienceLevel(profileUpdates.getExperienceLevel());
             if (StringUtils.hasText(profileUpdates.getEducationLevel())) existingProfile.setEducationLevel(profileUpdates.getEducationLevel());
        }

        // Atualizar configurações de acessibilidade (fazem parte da entidade User)
        // O valor booleano é sempre atualizado, pois não há como saber se "false" foi intencional ou ausência de dado
        existingUser.setLibrasAvatarEnabled(userUpdates.isLibrasAvatarEnabled());
        if (StringUtils.hasText(userUpdates.getPreferredTheme())) {
             existingUser.setPreferredTheme(userUpdates.getPreferredTheme());
        }


        // Save pode ser redundante
        return userRepository.save(existingUser);
    }

    @Override
    @Transactional // Operação de escrita
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) { // Verificar existência antes de deletar
             throw new EntityNotFoundException("Usuário não encontrado para exclusão com ID: " + id);
        }
        userRepository.deleteById(id);
    }

    // --- START: Accessibility Settings Specific Update Implementation ---
    @Override
    @Transactional // Operação de escrita
    public User updateAccessibilitySettings(UUID userId, boolean librasAvatarEnabled, String preferredTheme) {
        User user = findById(userId); // Reutiliza findById para buscar o usuário
        user.setLibrasAvatarEnabled(librasAvatarEnabled);

        // Validação básica do tema antes de definir
        if (StringUtils.hasText(preferredTheme) && List.of("default", "high-contrast-dark", "high-contrast-light").contains(preferredTheme)) {
            user.setPreferredTheme(preferredTheme);
        } else if (preferredTheme != null) {
            // Logar um aviso ou lançar exceção se o tema for inválido, dependendo da regra de negócio
            System.err.println("WARN: Tentativa de definir tema de acessibilidade inválido: " + preferredTheme);
            // Poderia lançar: throw new IllegalArgumentException("Tema de acessibilidade inválido: " + preferredTheme);
        }
        // Save pode ser redundante
        return userRepository.save(user);
    }
    // --- END: Accessibility Settings Specific Update Implementation ---
}