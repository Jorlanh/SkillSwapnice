package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import java.util.List;
import java.util.UUID;

public interface ChatService {
    ChatMessage sendMessage(ChatMessageRequestDTO messageRequest);
    // Novos métodos para voz e arquivos que recebem os dados e IDs
    ChatMessage sendVoiceMessage(UUID senderId, UUID receiverId, byte[] voiceData);
    ChatMessage sendFileMessage(UUID senderId, UUID receiverId, byte[] fileData, String fileType);
    List<ChatMessage> getChatHistory(UUID userId1, UUID userId2);
}