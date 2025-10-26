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
import br.com.teamss.skillswap.skill_swap.util.ByteArrayMultipartFile;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
// CORREÇÃO DE IMPORT: O Part correto vem da API, não do `generativeai`.
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ProposalRepository proposalRepository;
    private final UserServiceDTO userServiceDTO;
    private final FileUploadService fileUploadService;

    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gcp.project.id}")
    private String gcpProjectId;

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
            byte[] imageBytes = generateImageWithGemini(finalPrompt);
            String filename = "achievement_" + userAchievement.getId() + ".png";
            MultipartFile imageFile = new ByteArrayMultipartFile(imageBytes, "file", filename, "image/png");
            String imageUrl = fileUploadService.uploadFile(imageFile);
            
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new RuntimeException("A URL da imagem retornada pelo serviço de upload está vazia.");
            }

            userAchievement.setImageUrl(imageUrl);
            userAchievement.setCustomPrompt(request.getUserPrompt());
            return userAchievementRepository.save(userAchievement);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar ou fazer o upload da imagem da conquista: " + e.getMessage(), e);
        }
    }

    private byte[] generateImageWithGemini(String prompt) throws IOException {
        try (VertexAI vertexAI = new VertexAI(gcpProjectId, "us-central1")) {
            GenerativeModel model = new GenerativeModel("gemini-1.5-flash-001", vertexAI);
            
            GenerateContentResponse response = model.generateContent("Gere uma imagem com a seguinte descrição: " + prompt);

            // CORREÇÃO: Acessar os dados da imagem corretamente
            Part imagePart = response.getCandidates(0).getContent().getParts(0);
            if (imagePart.getInlineData() == null || imagePart.getInlineData().getData().isEmpty()) {
                throw new IOException("A API Gemini não retornou dados de imagem. Verifique o seu prompt ou as quotas da API.");
            }
            
            return imagePart.getInlineData().getData().toByteArray();
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