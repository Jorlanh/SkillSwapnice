package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebRTCConfig implements WebSocketConfigurer {

    @Bean
    public WebRTCVideoCallHandler webRTCVideoCallHandler() {
        return new WebRTCVideoCallHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        //o endpoint /video-call vai usar nosso handler de WebRTC, e deixa qualquer origem acessar
        registry.addHandler(webRTCVideoCallHandler(), "/video-call").setAllowedOrigins("*");
    }
}