package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    // Query personalizada pra buscar mensagens entre dois usuários (bidirecional)
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "(cm.sender.userId = :senderId1 AND cm.receiver.userId = :receiverId1) OR " +
           "(cm.sender.userId = :senderId2 AND cm.receiver.userId = :receiverId2)")
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverId(
        @Param("senderId1") UUID senderId1,         // ALTERADO DE Long PARA UUID
        @Param("receiverId1") UUID receiverId1,     // ALTERADO DE Long PARA UUID
        @Param("senderId2") UUID senderId2,         // ALTERADO DE Long PARA UUID
        @Param("receiverId2") UUID receiverId2);    // ALTERADO DE Long PARA UUID
}