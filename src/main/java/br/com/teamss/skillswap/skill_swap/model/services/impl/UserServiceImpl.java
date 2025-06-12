package br.com.teamss.skillswap.skill_swap.model.services.impl;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    @Override
    public User save(User user) {
        // tratar roles
        Set<Role> managedRoles = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                Role managedRole = roleRepository.findById(role.getRoleId())
                        .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + role.getRoleId()));
                managedRoles.add(managedRole);
            }
        }
        user.setRoles(managedRoles);

        // tratar skills
        Set<Skill> managedSkills = new HashSet<>();
        if (user.getSkills() != null) {
            for (Skill skill : user.getSkills()) {
                Skill managedSkill = skillRepository.findById(skill.getSkillId())
                        .orElseThrow(() -> new EntityNotFoundException("Skill não encontrada: " + skill.getSkillId()));
                managedSkills.add(managedSkill);
                managedSkill.getUsers().add(user);
            }
        }
        user.setSkills(managedSkills);

        // se for um perfil novo, salva ele primeiro para gerar id
        if (user.getProfile() != null) {
            if (user.getProfile().getProfileId() != null) {
                // perfil já existe, buscar pelo id
                // e associar ao usuário
                var managedProfile = profileRepository.findById(user.getProfile().getProfileId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Profile não encontrado: " + user.getProfile().getProfileId()));
                user.setProfile(managedProfile);
            } else {
                profileRepository.save(user.getProfile());
            }
            user.getProfile().setUser(user);
        }

        return userRepository.save(user);
    }

    @Override
    public User addSkills(UUID userId, List<Long> skillIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Set<Skill> managedSkills = skillIds.stream()
                .map(id -> skillRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Skill não encontrada: " + id)))
                .collect(Collectors.toSet());

        user.setSkills(managedSkills);
        return userRepository.save(user);
    }

    @Override
    public User addRoles(UUID userId, List<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Set<Role> managedRoles = roleIds.stream()
                .map(id -> roleRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + id)))
                .collect(Collectors.toSet());

        user.setRoles(managedRoles);
        return userRepository.save(user);
    }

    public User update(UUID id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        existingUser.setUsername(user.getUsername());
        existingUser.setPassword(user.getPassword());

        // Atualiza roles de forma segura
        if (user.getRoles() != null) {
            Set<Role> newRoles = new HashSet<>(user.getRoles());
            existingUser.setRoles(newRoles);
        }

        // Atualiza skills de forma segura
        if (user.getSkills() != null) {
            Set<Skill> newSkills = new HashSet<>(user.getSkills());
            existingUser.setSkills(newSkills);
        }

        // Atualiza profile se necessário
        if (user.getProfile() != null) {
            existingUser.setProfile(user.getProfile());
        }

        return userRepository.save(existingUser);
    }

    @Override
    public void delete(UUID id) {
        userRepository.deleteById(id);
    }
}
