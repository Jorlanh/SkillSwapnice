package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminService {
    List<UserManagementDTO> getAllUsers();
    UserManagementDTO toggleUserVerification(UUID userId);
    PlatformStatsDTO getPlatformStatistics();

    // Novos métodos
    void banUser(UUID userId, String reason, Instant expiresAt, String ipAddress);
    void unbanUser(UUID userId);
}