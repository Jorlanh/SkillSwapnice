package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.GenerateAchievementImageRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserAchievementDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Achievement;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.entities.UserAchievement;
import br.com.teamss.skillswap.skill_swap.model.repositories.AchievementRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserAchievementRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AchievementService;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.util.ByteArrayMultipartFile; // IMPORTAR A NOVA CLASSE
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.Part;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ProposalRepository proposalRepository;
    private final UserServiceDTO userServiceDTO;
    private final FileUploadService fileUploadService; // O seu serviço de upload (Cloudinary)

    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gcp.project.id}")
    private String gcpProjectId;
    
    // ... construtor e outros métodos ...
    
    @Override
    public UserAchievement generateAchievementImage(GenerateAchievementImageRequestDTO request) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        UserAchievement userAchievement = userAchievementRepository.findById(request.getAchievementId())
                .orElseThrow(() -> new IllegalStateException("Conquista de utilizador não encontrada."));

        if (!userAchievement.getUser().getUserId().equals(authenticatedUser.getUserId())) {
            throw new AccessDeniedException("Você não tem permissão para gerar a imagem para esta conquista.");
        }

        if (!"PENDING_GENERATION".equals(userAchievement.getImageUrl())) {
            throw new IllegalStateException("A imagem para esta conquista já foi gerada.");
        }

        String achievementName = userAchievement.getAchievement().getName();
        String finalPrompt = String.format(
            "%s. O nome da conquista é '%s'. Inclua este nome de forma visível na parte inferior da imagem.",
            request.getUserPrompt(), achievementName
        );

        try {
            // PASSO 1: Gerar a imagem com a Gemini e obter os bytes
            byte[] imageBytes = generateImageWithGemini(finalPrompt);

            // PASSO 2: Converter os bytes num objeto MultipartFile
            String filename = "achievement_" + userAchievement.getId() + ".png";
            MultipartFile imageFile = new ByteArrayMultipartFile(imageBytes, "file", filename, "image/png");

            // PASSO 3: Fazer o upload da imagem usando o seu serviço existente
            String imageUrl = fileUploadService.uploadFile(imageFile);
            
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new RuntimeException("A URL da imagem retornada pelo serviço de upload está vazia.");
            }

            // PASSO 4: Salvar a URL final na base de dados
            userAchievement.setImageUrl(imageUrl);
            userAchievement.setCustomPrompt(request.getUserPrompt());
            return userAchievementRepository.save(userAchievement);

        } catch (Exception e) {
            // Lançar uma exceção mais específica se o upload falhar
            throw new RuntimeException("Falha ao gerar ou fazer o upload da imagem da conquista: " + e.getMessage(), e);
        }
    }
    
    // ... resto dos métodos ...

    public AchievementServiceImpl(AchievementRepository achievementRepository, UserAchievementRepository userAchievementRepository,
                                  ProposalRepository proposalRepository, UserServiceDTO userServiceDTO, FileUploadService fileUploadService) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.proposalRepository = proposalRepository;
        this.userServiceDTO = userServiceDTO;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public void checkAndUnlockAchievements(User user) {
        checkTradeAchievements(user, "TRADES_10", 10);
    }

    private void checkTradeAchievements(User user, String achievementKey, int requiredTrades) {
        achievementRepository.findByAchievementKey(achievementKey).ifPresent(achievement -> {
            boolean alreadyUnlocked = userAchievementRepository.existsByUser_UserIdAndAchievement_Id(user.getUserId(), achievement.getId());
            if (!alreadyUnlocked) {
                long completedTrades = proposalRepository.countByStatusAndParticipant("COMPLETED", user.getUserId());
                if (completedTrades >= requiredTrades) {
                    UserAchievement userAchievement = new UserAchievement();
                    userAchievement.setUser(user);
                    userAchievement.setAchievement(achievement);
                    userAchievement.setImageUrl("PENDING_GENERATION");
                    userAchievementRepository.save(userAchievement);
                }
            }
        });
    }
    
    private byte[] generateImageWithGemini(String prompt) throws IOException {
        try (VertexAI vertexAI = new VertexAI(gcpProjectId, "us-central1")) {
            GenerativeModel model = new GenerativeModel("gemini-1.5-flash-001", vertexAI);
            // IMPORTANTE: O prompt para gerar imagens pode ser diferente de gerar texto.
            // A API de imagem da Gemini (Imagen) pode requerer uma chamada ligeiramente diferente.
            // Este código assume que o modelo multimodal consegue retornar a imagem diretamente.
            // Se encontrar erros aqui, pode ser necessário ajustar para a API específica de geração de imagem.
            GenerateContentResponse response = model.generateContent("Gere uma imagem com a seguinte descrição: " + prompt);

            Part imagePart = response.getCandidates(0).getContent().getParts(0);
            if (imagePart.getBlob() == null || imagePart.getBlob().getData().isEmpty()) {
                throw new IOException("A API Gemini não retornou dados de imagem.");
            }
            
            // A API retorna os bytes diretamente, não em base64 neste SDK.
            return imagePart.getBlob().getData().toByteArray();
        }
    }

    @Override
    public List<UserAchievementDTO> getUserAchievements(UUID userId) {
        return userAchievementRepository.findByUser_UserId(userId).stream()
                .map(ua -> new UserAchievementDTO(
                        ua.getAchievement().getName(),
                        ua.getAchievement().getDescription(),
                        ua.getImageUrl(),
                        ua.getUnlockedAt()))
                .collect(Collectors.toList());
    }
}