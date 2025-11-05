package br.com.teamss.skillswap.skill_swap.model.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ChannelTopic chatTopic;

    @Autowired
    private ObjectMapper objectMapper;

    // Chaves do Redis para mapear usuário -> sessão
    private static final String REDIS_KEY_USER_TO_SESSION = "skillswap:chat:user_sessions";
    
    // Mapeia o ID da sessão para a sessão (apenas sessões vivas NESTA instância)
    private final Map<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();
    // Mapeia o ID da sessão para o ID do usuário (apenas sessões vivas NESTA instância)
    private final Map<String, String> localSessionToUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // O usuário deve se autenticar enviando uma mensagem {"type": "auth", "userId": "..."}
        // Por enquanto, apenas armazenamos localmente.
        localSessions.put(session.getId(), session);
        System.out.println("Sessão de Chat local estabelecida: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode jsonMessage = objectMapper.readTree(message.getPayload());
        String type = jsonMessage.has("type") ? jsonMessage.get("type").asText() : null;

        if ("auth".equals(type) && jsonMessage.has("userId")) {
            // Autentica a sessão localmente e no Redis
            String userId = jsonMessage.get("userId").asText();
            localSessionToUser.put(session.getId(), userId);
            // Armazena no Redis: "userId" -> "sessionId"
            redisTemplate.opsForHash().put(REDIS_KEY_USER_TO_SESSION, userId, session.getId());
            System.out.println("Sessão " + session.getId() + " autenticada como usuário " + userId);
            return;
        }

        // Publica a mensagem no Redis para que todos os servidores (incluindo este) a recebam
        // O subscriber (RedisMessageSubscriber) cuidará da distribuição
        redisTemplate.convertAndSend(chatTopic.getTopic(), message.getPayload());
    }

    /**
     * Este método é chamado pelo RedisMessageSubscriber quando uma mensagem
     * chega em *qualquer* servidor.
     */
    public void broadcastMessageToLocalSessions(String messagePayload) {
        try {
            JsonNode message = objectMapper.readTree(messagePayload);
            String receiverId = message.has("receiverId") ? message.get("receiverId").asText() : null;
            
            if (receiverId == null) return; // Não é uma mensagem de chat direcionada

            // Verifica no Redis qual sessão pertence a este usuário
            String targetSessionId = (String) redisTemplate.opsForHash().get(REDIS_KEY_USER_TO_SESSION, receiverId);

            if (targetSessionId != null && localSessions.containsKey(targetSessionId)) {
                // A sessão do destinatário está NESTA instância do servidor
                WebSocketSession clientSession = localSessions.get(targetSessionId);
                if (clientSession != null && clientSession.isOpen()) {
                    clientSession.sendMessage(new TextMessage(messagePayload));
                }
            }
        } catch (Exception e) {
            System.err.println("Falha ao processar broadcast do Chat via Redis: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        localSessions.remove(sessionId); 

        // Remove do Redis
        String userId = localSessionToUser.remove(sessionId);
        if (userId != null) {
            try {
                // Remove apenas se o ID da sessão for o mesmo (evita condição de corrida)
                if (sessionId.equals(redisTemplate.opsForHash().get(REDIS_KEY_USER_TO_SESSION, userId))) {
                    redisTemplate.opsForHash().delete(REDIS_KEY_USER_TO_SESSION, userId);
                }
            } catch (Exception e) {
                 System.err.println("Erro ao limpar sessão de chat do Redis: " + sessionId);
            }
        }
        System.out.println("Sessão de Chat fechada: " + sessionId);
    }
}