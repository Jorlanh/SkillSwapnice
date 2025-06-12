package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.AccessLogDTO;
import br.com.teamss.skillswap.skill_swap.model.services.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/access-logs")
public class AccessLogController {

    @Autowired
    private AccessLogService accessLogService;

    // Endpoint para consultar o histórico de acessos
    @GetMapping("/{userId}")
    public ResponseEntity<List<AccessLogDTO>> getAccessHistory(@PathVariable String userId) {
        UUID uuid = UUID.fromString(userId);
        List<AccessLogDTO> history = accessLogService.getAccessHistory(uuid);
        return ResponseEntity.ok(history);
    }
}