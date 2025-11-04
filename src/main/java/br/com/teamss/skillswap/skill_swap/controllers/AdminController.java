package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.BanRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/console-management")
// ALTERAÇÃO PRINCIPAL: Protege todos os endpoints neste controller.
// Apenas tokens com a permissão "manage:console" poderão aceder.
@PreAuthorize("hasAuthority('manage:console')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDTO> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStatistics());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserManagementDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users/{userId}/toggle-verification")
    public ResponseEntity<UserManagementDTO> toggleVerification(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.toggleUserVerification(userId));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable UUID userId, @RequestBody BanRequestDTO banRequest) {
        adminService.banUser(userId, banRequest.getReason(), banRequest.getExpiresAt(), banRequest.getIpAddress());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID userId) {
        adminService.unbanUser(userId);
        return ResponseEntity.ok().build();
    }
}