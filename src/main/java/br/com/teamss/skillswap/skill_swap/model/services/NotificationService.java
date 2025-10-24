package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.NotificationDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDTO> getUnreadNotifications(UUID userId);
    void markAsRead(Long notificationId);
    void createNotification(UUID userId, String message);
}