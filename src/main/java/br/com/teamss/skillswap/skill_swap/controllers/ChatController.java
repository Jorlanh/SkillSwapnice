package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.services.ChatService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // INJEÇÃO DO SERVICE PARA OBTER O USUÁRIO AUTENTICADO DE FORMA SEGURA
    @Autowired
    private UserServiceDTO userServiceDTO;

    @PostMapping("/send")
    public ChatMessage sendMessage(@RequestBody ChatMessageRequestDTO messageRequest) {
        // **CORREÇÃO DE SEGURANÇA (IDOR):** Valida se o senderId da requisição é o mesmo do usuário autenticado.
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        if (!authenticatedUser.getUserId().equals(messageRequest.getSenderId())) {
            throw new AccessDeniedException("Não é permitido enviar mensagens em nome de outro usuário.");
        }
        return chatService.sendMessage(messageRequest);
    }

    @PostMapping("/send-voice")
    public ChatMessage sendVoiceMessage(@RequestParam("voice") MultipartFile voiceFile, @RequestParam UUID receiverId) { // REMOVIDO @RequestParam UUID senderId
        // **CORREÇÃO DE SEGURANÇA (IDOR):** O senderId é obtido do token, não do cliente.
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        UUID senderId = authenticatedUser.getUserId();

        if (voiceFile.getSize() > 10485760) { // 10MB limite
            throw new IllegalArgumentException("Voice file size exceeds 10MB");
        }
        try {
            return chatService.sendVoiceMessage(senderId, receiverId, voiceFile.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to process voice file: " + e.getMessage(), e);
        }
    }

    @PostMapping("/upload-file")
    public ChatMessage uploadFile(@RequestParam("file") MultipartFile file, @RequestParam UUID receiverId) { // REMOVIDO @RequestParam UUID senderId
        // **CORREÇÃO DE SEGURANÇA (IDOR):** O senderId é obtido do token, não do cliente.
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        UUID senderId = authenticatedUser.getUserId();

        long fileSizeLimit = getFileSizeLimit(file.getContentType());
        if (file.getSize() > fileSizeLimit) {
            throw new IllegalArgumentException("File size exceeds limit for type: " + file.getContentType());
        }
        try {
            return chatService.sendFileMessage(senderId, receiverId, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
        }
    }

    @GetMapping("/history/{userId1}/{userId2}")
    public List<ChatMessage> getChatHistory(@PathVariable UUID userId1, @PathVariable UUID userId2) {
        return chatService.getChatHistory(userId1, userId2);
    }

    private long getFileSizeLimit(String contentType) {
        if (contentType != null && contentType.contains("image")) return 10485760; // 10 MB
        if (contentType != null && contentType.contains("video")) return 104857600; // 100MB
        return 20971520; // 20MB para outros
    }
}