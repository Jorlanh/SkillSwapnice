package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.VideoGrant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private UserServiceDTO userServiceDTO;

    // Injetado do LiveKitConfig
    private RoomServiceClient roomServiceClient; 

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    @Autowired // CORREÇÃO: Removido @Autowired desnecessário do construtor
    public VideoController(UserServiceDTO userServiceDTO, RoomServiceClient roomServiceClient) {
        this.userServiceDTO = userServiceDTO;
        this.roomServiceClient = roomServiceClient;
    }

    /**
     * Endpoint para o cliente (Angular) solicitar um token para entrar em uma sala de vídeo.
     */
    @PostMapping("/join-room")
    @PreAuthorize("isAuthenticated()") // Garante que o usuário está logado
    public ResponseEntity<?> getJoinToken(@RequestBody Map<String, String> request) {
        String roomName = request.get("roomName");
        if (roomName == null || roomName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roomName é obrigatório."));
        }

        // 1. Pega o usuário autenticado (via OAuth 2.0)
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        
        // 2. CORREÇÃO: A API do LiveKit 1.5.0 usa 'new VideoGrant()' e métodos 'set...'
        VideoGrant videoGrant = new VideoGrant();
        videoGrant.setRoom(roomName);
        videoGrant.setRoomJoin(true);
        videoGrant.setCanPublish(true); // Pode ligar a câmera/microfone
        videoGrant.setCanSubscribe(true); // Pode ver/ouvir os outros
        videoGrant.setCanPublishData(true); // Pode usar o datachannel (chat da sala)
        videoGrant.setCanPublishSources(List.of("camera", "microphone", "screen_share")); // Permite compartilhar tela
        videoGrant.setHidden(false);
        
        // 3. CORREÇÃO: A API do LiveKit 1.5.0 usa um Builder para o AccessToken
        AccessToken.Builder builder = new AccessToken.Builder()
            .withApiKey(livekitApiKey)
            .withApiSecret(livekitApiSecret)
            .withIdentity(authenticatedUser.getUserId().toString())
            .withName(authenticatedUser.getUsername())
            .withTtl(Duration.ofHours(1)) // Define a duração
            .withGrant(videoGrant); // Adiciona as permissões
            
        // 4. Constroí o token
        AccessToken token = builder.build();

        // 5. Retorna o token JWT (do LiveKit) para o cliente
        return ResponseEntity.ok(Map.of("token", token.toJwt()));
    }
}