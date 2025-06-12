package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // Definir o ConcurrentHashMap como um bean
    @Bean
    public ConcurrentHashMap<String, WebSocketSession> webSocketSessions() {
        return new ConcurrentHashMap<>();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Injetar o ConcurrentHashMap no VideoCallHandler
        registry.addHandler(new VideoCallHandler(webSocketSessions()), "/video-call").setAllowedOrigins("*");

        // Adicionado: Configurar CORS de forma mais segura (opcional, ajuste conforme necessário)
        registry.addHandler(videoCallHandler(), "/video-call").setAllowedOrigins("http://localhost:3000", "https://yourdomain.com");
    }

    // Adicionado: Definir VideoCallHandler como um bean Spring
    @Bean
    public VideoCallHandler videoCallHandler() {
        return new VideoCallHandler(webSocketSessions());
    }
}