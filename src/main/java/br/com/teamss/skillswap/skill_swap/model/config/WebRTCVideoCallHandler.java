package br.com.teamss.skillswap.skill_swap.model.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebRTCVideoCallHandler extends TextWebSocketHandler {

    // Mapeia o ID da sessão para a própria sessão
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // Mapeia o ID da sessão para o ID da sala em que o usuário está
    private final Map<String, String> sessionToRoomMap = new ConcurrentHashMap<>();
    // Mapeia o ID da sala para uma lista de sessões naquela sala
    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("Nova conexão WebSocket estabelecida: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode jsonMessage = objectMapper.readTree(message.getPayload());
        String type = jsonMessage.has("type") ? jsonMessage.get("type").asText() : null;

        if ("join_room".equals(type)) {
            handleJoinRoom(session, jsonMessage);
        } else if (type != null && jsonMessage.has("roomId")) {
            String roomId = jsonMessage.get("roomId").asText();
            forwardMessageToRoom(session, roomId, message);
        } else {
            System.err.println("Mensagem sem 'type' ou 'roomId' recebida: " + message.getPayload());
        }
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode jsonMessage) {
        String roomId = jsonMessage.get("roomId").asText();
        String sessionId = session.getId();

        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(sessionId, session);
        sessionToRoomMap.put(sessionId, roomId);

        System.out.println("Sessão " + sessionId + " entrou na sala " + roomId);
    }

    private void forwardMessageToRoom(WebSocketSession senderSession, String roomId, TextMessage message) throws IOException {
        Map<String, WebSocketSession> roomSessions = rooms.get(roomId);
        if (roomSessions != null) {
            for (WebSocketSession clientSession : roomSessions.values()) {
                // Envia a mensagem para todos na sala, exceto para o remetente
                if (clientSession.isOpen() && !clientSession.getId().equals(senderSession.getId())) {
                    clientSession.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        String roomId = sessionToRoomMap.remove(sessionId);
        if (roomId != null) {
            Map<String, WebSocketSession> roomSessions = rooms.get(roomId);
            if (roomSessions != null) {
                roomSessions.remove(sessionId);
                if (roomSessions.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        }
        System.out.println("Conexão WebSocket fechada: " + sessionId + ". Removido da sala: " + roomId);
    }
}