package br.com.teamss.skillswap.skill_swap.model.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    // Defina estas 3 variáveis no seu application.properties ou variáveis de ambiente
    @Value("${livekit.api.url}")
    private String livekitUrl; // Ex: "http://localhost:7880"

    @Value("${livekit.api.key}")
    private String livekitApiKey; // Ex: "devkey"

    @Value("${livekit.api.secret}")
    private String livekitApiSecret; // Ex: "secret"

    @Bean
    public RoomServiceClient roomServiceClient() {
        // CORREÇÃO: O construtor é privado, usa-se o Builder.
        return new RoomServiceClient.Builder()
                .withUrl(livekitUrl)
                .withApiKey(livekitApiKey)
                .withApiSecret(livekitApiSecret)
                .build();
    }
}