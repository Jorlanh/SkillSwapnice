package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ProposalRepository proposalRepository;

    public AdminServiceImpl(UserRepository userRepository, SkillRepository skillRepository, ProposalRepository proposalRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.proposalRepository = proposalRepository;
    }

    @Override
    public List<UserManagementDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserManagementDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserManagementDTO toggleUserVerification(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + userId));
        
        user.setVerifiedBadge(!user.isVerifiedBadge());
        User updatedUser = userRepository.save(user);

        return convertToUserManagementDTO(updatedUser);
    }

    @Override
    public PlatformStatsDTO getPlatformStatistics() {
        long totalUsers = userRepository.count();
        long totalSkills = skillRepository.count();
        long totalProposals = proposalRepository.count();
        
        // Contagem de propostas por status
        // AVISO: O método countByStatusAndParticipant espera um UUID. 
        // Para uma contagem geral, o ideal seria ter um método sem o participante.
        // Assumindo que podemos adaptar ou criar um novo método no repositório.
        // Por agora, vamos usar um método que não precise do UUID.
        long pendingProposals = proposalRepository.countByStatus("PENDING");
        long completedProposals = proposalRepository.countByStatus("COMPLETED");

        return new PlatformStatsDTO(totalUsers, totalSkills, totalProposals, pendingProposals, completedProposals);
    }

    private UserManagementDTO convertToUserManagementDTO(User user) {
        return new UserManagementDTO(
                user.getUserId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.isVerifiedBadge(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}