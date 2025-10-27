package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Ban;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.BanRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ProposalRepository proposalRepository;
    private final BanRepository banRepository;

    public AdminServiceImpl(UserRepository userRepository, SkillRepository skillRepository,
                            ProposalRepository proposalRepository, BanRepository banRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.proposalRepository = proposalRepository;
        this.banRepository = banRepository;
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
        
        long pendingProposals = proposalRepository.countByStatus("PENDING");
        long completedProposals = proposalRepository.countByStatus("COMPLETED");

        return new PlatformStatsDTO(totalUsers, totalSkills, totalProposals, pendingProposals, completedProposals);
    }

    @Override
    public void banUser(UUID userId, String reason, Instant expiresAt, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + userId));

        user.setBanned(true);
        userRepository.save(user);

        Ban ban = new Ban();
        ban.setUser(user);
        ban.setReason(reason);
        ban.setExpiresAt(expiresAt);
        ban.setIpAddress(ipAddress);
        banRepository.save(ban);
    }

    @Override
    public void unbanUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + userId));
        
        user.setBanned(false);
        userRepository.save(user);

        List<Ban> activeBans = banRepository.findByUser_UserIdAndActiveTrue(userId);
        activeBans.forEach(ban -> ban.setActive(false));
        banRepository.saveAll(activeBans);
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