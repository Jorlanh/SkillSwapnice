package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.TranslationService;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GeminiTranslationServiceImpl implements TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiTranslationServiceImpl.class); // Logger instance

    @Value("${gcp.project.id}")
    private String gcpProjectId;

    @Value("${gcp.location}") // Assuming you have location in properties, default if not
    private String gcpLocation = "us-central1"; // Default location

    @Value("${gemini.model.name:gemini-1.5-flash-001}") // Allow model override from properties
    private String geminiModelName;


    @Override
    public String translate(String text, String targetLanguage) {
        if (text == null || text.isBlank() || targetLanguage == null || targetLanguage.isBlank()) {
            logger.warn("Texto ou idioma de destino inválido para tradução.");
            return text; // Return original text if input is invalid
        }
        String prompt = String.format("Translate the following text to %s: \"%s\"", targetLanguage, text);
        return callGemini(prompt, text); // Pass original text as fallback
    }

    @Override
    public String simplifyText(String text) {
        if (text == null || text.isBlank()) {
             logger.warn("Texto inválido para simplificação.");
            return text; // Return original text if input is invalid
        }
        String prompt = String.format(
            "Reescreva o seguinte texto em uma linguagem simples e fácil de entender, " +
            "ideal para pessoas com dificuldades de leitura ou aprendizado. " +
            "Use frases curtas, vocabulário comum e evite jargões: \"%s\"", text
        );
        return callGemini(prompt, text); // Pass original text as fallback
    }

    /**
     * Helper method to call the Gemini API.
     * @param prompt The prompt to send to the model.
     * @param fallbackText The text to return in case of an error.
     * @return The model's response or the fallback text.
     */
    private String callGemini(String prompt, String fallbackText) {
         if (gcpProjectId == null || gcpProjectId.isBlank()) {
            logger.error("GCP Project ID não está configurado. Não é possível chamar a API Gemini.");
            return fallbackText;
        }

        try (VertexAI vertexAI = new VertexAI(gcpProjectId, gcpLocation)) {
            GenerativeModel model = new GenerativeModel(geminiModelName, vertexAI);
            logger.debug("Enviando prompt para Gemini ({}): {}", geminiModelName, prompt); // Log prompt
            GenerateContentResponse response = model.generateContent(prompt);
            String result = ResponseHandler.getText(response);
            logger.debug("Resposta recebida do Gemini: {}", result); // Log response
            return result;
        } catch (IOException e) {
            logger.error("Erro ao chamar a API Gemini: {}", e.getMessage(), e); // Log detailed error
            return fallbackText; // Return original/fallback text on error
        } catch (Exception e) {
             logger.error("Erro inesperado ao interagir com a API Gemini: {}", e.getMessage(), e);
             return fallbackText;
        }
    }
}
