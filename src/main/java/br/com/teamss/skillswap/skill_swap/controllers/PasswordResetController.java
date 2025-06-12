package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.PasswordResetDTO;
import br.com.teamss.skillswap.skill_swap.dto.PasswordResetRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.PasswordResetVerifyDTO;
import br.com.teamss.skillswap.skill_swap.model.services.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    // Endpoint para enviar o código de verificação
    @PostMapping("/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDTO requestDTO) {
        try {
            passwordResetService.requestPasswordReset(requestDTO.getEmail());
            return ResponseEntity.ok(new SuccessResponse("Código de verificação enviado para o e-mail."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Endpoint para verificar o código
    @PostMapping("/verify")
    public ResponseEntity<?> verifyResetCode(@RequestBody PasswordResetVerifyDTO verifyDTO) {
        boolean isValid = passwordResetService.verifyResetCode(verifyDTO.getEmail(), verifyDTO.getCode());
        if (!isValid) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Código de verificação inválido ou expirado."));
        }
        return ResponseEntity.ok(new SuccessResponse("Código verificado com sucesso."));
    }

    // Endpoint para alterar a senha
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetDTO resetDTO) {
        try {
            passwordResetService.resetPassword(resetDTO.getEmail(), resetDTO.getNewPassword(), resetDTO.getConfirmPassword());
            return ResponseEntity.ok(new SuccessResponse("Senha alterada com sucesso."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}

// Classes de resposta (reutilizadas de outros controladores)
class ErrorResponse {
    private String message;
    public ErrorResponse(String message) { this.message = message; }
    public String getMessage() { return message; }
}

class SuccessResponse {
    private String message;
    public SuccessResponse(String message) { this.message = message; }
    public String getMessage() { return message; }
}