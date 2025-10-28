package br.com.teamss.skillswap.skill_swap.model.services.impl; // Changed package

import br.com.teamss.skillswap.skill_swap.dto.ChatbotRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ChatbotService; // Import the interface
import br.com.teamss.skillswap.skill_swap.model.services.TranslationService;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate; // Import Candidate
import com.google.cloud.vertexai.api.Content; // Correct import for Content
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part; // Correct import for Part
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler; // Keep ResponseHandler for getText
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Removed Autowired import as it's not needed on constructor
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ChatbotServiceImpl implements ChatbotService { // Implement the interface

    private static final Logger logger = LoggerFactory.getLogger(ChatbotServiceImpl.class);

    private final TranslationService translationService; // Use final and constructor injection

    @Value("${gcp.project.id}")
    private String gcpProjectId;

    @Value("${gcp.location:us-central1}")
    private String gcpLocation;

    @Value("${gemini.model.name:gemini-1.5-flash-001}")
    private String geminiModelName;

    // @Autowired removed - unnecessary for constructor injection with one constructor
    public ChatbotServiceImpl(TranslationService translationService) {
        this.translationService = translationService;
    }

    @Override // Add Override annotation
    public ChatbotResponseDTO getChatbotResponse(ChatbotRequestDTO requestDTO) {
        if (gcpProjectId == null || gcpProjectId.isBlank()) {
            logger.error("GCP Project ID não está configurado. Chatbot indisponível.");
            return new ChatbotResponseDTO("Desculpe, o chatbot está temporariamente indisponível.", null, false);
        }
        if (requestDTO == null || requestDTO.getMessage() == null || requestDTO.getMessage().isBlank()) {
             logger.warn("Recebida requisição de chatbot inválida (mensagem vazia).");
             return new ChatbotResponseDTO("Por favor, envie uma mensagem.", null, false);
        }

        try (VertexAI vertexAI = new VertexAI(gcpProjectId, gcpLocation)) {
            // Define system instructions for the chatbot's persona and task
            String systemPrompt = "Você é um assistente prestativo da plataforma SkillSwap. " +
                                  "Seu objetivo é ajudar os usuários a navegar na plataforma, encontrar informações sobre habilidades, " +
                                  "entender como funcionam as trocas e responder perguntas gerais sobre o SkillSwap. " +
                                  "Seja amigável, claro e conciso. Responda sempre em português brasileiro.";

            // Build the system instruction Content object
            Content systemInstruction = Content.newBuilder()
                .addParts(Part.newBuilder().setText(systemPrompt).build())
                .build();

            GenerativeModel model = new GenerativeModel(geminiModelName, vertexAI)
                .withSystemInstruction(systemInstruction);

            // For stateless chat, simply generate content based on the user message
            logger.debug("Enviando mensagem para o chatbot Gemini: '{}'", requestDTO.getMessage());
            GenerateContentResponse response = model.generateContent(requestDTO.getMessage());

            // --- CORREÇÃO APLICADA AQUI ---
            // Validate response candidates and parts before extracting text
            String rawResponse = ""; // Inicializa a resposta
            boolean responseHasText = false;
            if (response.getCandidatesCount() > 0) {
                 Candidate candidate = response.getCandidates(0);
                 if (candidate.getContent() != null && candidate.getContent().getPartsCount() > 0) {
                     Part firstPart = candidate.getContent().getParts(0);
                     if (firstPart.hasText() && !firstPart.getText().isBlank()) {
                         rawResponse = firstPart.getText(); // Extrai texto da primeira parte
                         responseHasText = true;
                     }
                 }
            }

            if (!responseHasText) {
                logger.warn("Resposta do Gemini não contém texto utilizável. Resposta completa: {}", response);
                String finishReason = response.getCandidatesCount() > 0 ? response.getCandidates(0).getFinishReason().name() : "UNKNOWN";
                if ("SAFETY".equals(finishReason)) {
                    return new ChatbotResponseDTO("Desculpe, não posso responder a essa pergunta devido às políticas de segurança.", null, false);
                } else {
                    return new ChatbotResponseDTO("Desculpe, não consegui gerar uma resposta neste momento (Razão: " + finishReason + ").", null, false);
                }
            }
             // --- FIM DA CORREÇÃO ---


            logger.debug("Resposta bruta do chatbot Gemini: '{}'", rawResponse);

            String finalResponse = rawResponse;
            boolean wasSimplified = false;

            // Simplify the response if requested
            if (requestDTO.isSimplifyResponse()) {
                logger.debug("Solicitada simplificação da resposta.");
                if (translationService != null) {
                    finalResponse = translationService.simplifyText(rawResponse);
                    wasSimplified = !finalResponse.equals(rawResponse);
                    logger.debug("Resposta simplificada: '{}'", finalResponse);
                } else {
                    logger.warn("TranslationService não injetado, não é possível simplificar a resposta.");
                }
            }

            // Context is null for stateless chat
            return new ChatbotResponseDTO(finalResponse, null, wasSimplified);

        } catch (IOException e) {
            logger.error("Erro de IO ao comunicar com a API Vertex AI (Gemini): {}", e.getMessage(), e);
            return new ChatbotResponseDTO("Desculpe, ocorreu um erro de comunicação ao processar sua solicitação.", null, false);
        } catch (Exception e) {
            logger.error("Erro inesperado no serviço de chatbot: {}", e.getMessage(), e);
            return new ChatbotResponseDTO("Desculpe, ocorreu um erro inesperado. Tente novamente mais tarde.", null, false);
        }
    }
}