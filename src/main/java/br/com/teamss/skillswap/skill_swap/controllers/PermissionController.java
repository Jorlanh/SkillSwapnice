package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.model.services.PermissionService;
import br.com.teamss.skillswap.skill_swap.model.entities.PermissionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    // Endpoint para alternar o estado da permissão (ativa/desativa o botão)
    @PostMapping("/toggle/{permissionType}")
    public ResponseEntity<?> togglePermission(@PathVariable String permissionType, @RequestParam String userId) {
        permissionService.togglePermission(permissionType, userId);
        PermissionStatus status = permissionService.getPermissionStatus(permissionType, userId);
        return ResponseEntity.ok(new PermissionResponse(status));
    }

    // Endpoint para definir a escolha do usuário ao clicar no botão de ativação
    @PostMapping("/choose/{permissionType}")
    public ResponseEntity<?> setPermissionChoice(
            @PathVariable String permissionType,
            @RequestParam String userId,
            @RequestParam String choice) {
        permissionService.setPermissionChoice(permissionType, userId, choice);
        PermissionStatus status = permissionService.getPermissionStatus(permissionType, userId);
        return ResponseEntity.ok(new PermissionResponse(status));
    }
}

// Classe de resposta para o frontend
class PermissionResponse {
    private PermissionStatus status;

    public PermissionResponse(PermissionStatus status) {
        this.status = status;
    }

    public PermissionStatus getStatus() {
        return status;
    }
}