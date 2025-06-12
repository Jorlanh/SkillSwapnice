package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ChatRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.ChatService;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserRepository userRepository;

    @Override
    public ChatMessage sendMessage(ChatMessageRequestDTO messageRequest) {
        User sender = userRepository.findById(messageRequest.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("Remetente não encontrado"));
        User receiver = userRepository.findById(messageRequest.getReceiverId())
                .orElseThrow(() -> new EntityNotFoundException("Destinatário não encontrado"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageRequest.getContent());

        return saveAndNotify(message);
    }

    @Override
    public ChatMessage sendVoiceMessage(UUID senderId, UUID receiverId, byte[] voiceData) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Remetente não encontrado"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Destinatário não encontrado"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setVoiceData(voiceData);

        return saveAndNotify(message);
    }
    
    @Override
    public ChatMessage sendFileMessage(UUID senderId, UUID receiverId, byte[] fileData, String fileType) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Remetente não encontrado"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Destinatário não encontrado"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setFileData(fileData);
        message.setFileType(fileType);
        
        return saveAndNotify(message);
    }
    
    // Método privado para evitar repetição de código
    private ChatMessage saveAndNotify(ChatMessage message) {
        ChatMessage savedMessage = chatRepository.save(message);

        String receiverEmail = savedMessage.getReceiver().getEmail();
        String messageContent = "Você recebeu uma nova mensagem de " + savedMessage.getSender().getUsername() + ".";
        
        emailService.sendNotification(receiverEmail, "Nova Mensagem no SkillSwap", messageContent);
        emailService.sendPlatformNotification("Nova mensagem para " + receiverEmail);
        
        Notification notification = new Notification();
        notification.setUser(savedMessage.getReceiver());
        notification.setMessage(messageContent);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        return savedMessage;
    }

     @Override
    public List<ChatMessage> getChatHistory(UUID userId1, UUID userId2) {
        // A query agora espera UUIDs, então esta chamada vai funcionar.
        return chatRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverId(userId1, userId2, userId2, userId1);
    }
}