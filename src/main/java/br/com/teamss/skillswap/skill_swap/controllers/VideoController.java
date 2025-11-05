package br.com.teamss.skillswap.skill_swap.controllers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.VideoGrant;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final UserServiceDTO userServiceDTO;
    private final RoomServiceClient roomServiceClient;

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    // ✅ O Spring injeta automaticamente pelo construtor
    public VideoController(UserServiceDTO userServiceDTO, RoomServiceClient roomServiceClient) {
        this.userServiceDTO = userServiceDTO;
        this.roomServiceClient = roomServiceClient;
    }

    /**
     * Endpoint para gerar o token de acesso ao LiveKit.
     * O cliente (ex: Angular) chama este endpoint ao entrar em uma sala.
     */
    @PostMapping("/join-room")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getJoinToken(@RequestBody Map<String, String> request) {
        String roomName = request.get("roomName");
        if (roomName == null || roomName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roomName é obrigatório."));
        }

        // Pega o usuário autenticado via UserServiceDTO
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();

        try {
            // ✅ Criação direta do VideoGrant (sem builder)
            VideoGrant videoGrant = new VideoGrant();
            videoGrant.setRoom(roomName);
            videoGrant.setRoomJoin(true);
            videoGrant.setCanPublish(true);
            videoGrant.setCanSubscribe(true);
            videoGrant.setCanPublishData(true);
            videoGrant.setCanPublishSources(List.of("camera", "microphone", "screen_share"));
            videoGrant.setHidden(false);

            // ✅ Cria o AccessToken e adiciona o grant
            AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret)
                    .setIdentity(authenticatedUser.getUserId().toString())
                    .setName(authenticatedUser.getUsername())
                    .setTtl(Duration.ofHours(1))
                    .addGrant(videoGrant);

            // ✅ Gera o JWT
            String jwt = token.toJwt();

            // Retorna o token JWT para o cliente
            return ResponseEntity.ok(Map.of("token", jwt));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Falha ao gerar token: " + e.getMessage()
            ));
        }
    }
}
