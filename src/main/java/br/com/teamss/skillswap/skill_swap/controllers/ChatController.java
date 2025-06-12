package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
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

    // Este método agora usa o DTO
    @PostMapping("/send")
    public ChatMessage sendMessage(@RequestBody ChatMessageRequestDTO messageRequest) {
        return chatService.sendMessage(messageRequest);
    }

    // Corrigido para receber UUID e chamar o serviço correto
    @PostMapping("/send-voice")
    public ChatMessage sendVoiceMessage(@RequestParam("voice") MultipartFile voiceFile, @RequestParam UUID senderId, @RequestParam UUID receiverId) {
        if (voiceFile.getSize() > 10485760) { // 10MB limite
            throw new IllegalArgumentException("Voice file size exceeds 10MB");
        }
        try {
            return chatService.sendVoiceMessage(senderId, receiverId, voiceFile.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to process voice file: " + e.getMessage(), e);
        }
    }

    // Corrigido para receber UUID e chamar o serviço correto
    @PostMapping("/upload-file")
    public ChatMessage uploadFile(@RequestParam("file") MultipartFile file, @RequestParam UUID senderId, @RequestParam UUID receiverId) {
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

    // Corrigido para usar UUID
    @GetMapping("/history/{userId1}/{userId2}")
    public List<ChatMessage> getChatHistory(@PathVariable UUID userId1, @PathVariable UUID userId2) {
        return chatService.getChatHistory(userId1, userId2);
    }

    private long getFileSizeLimit(String contentType) {
        if (contentType.contains("image")) return 10485760; // 10 MB
        if (contentType.contains("video")) return 104857600; // 100MB
        return 20971520; // 20MB para outros
    }
}