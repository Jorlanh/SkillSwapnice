package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.TranslationService;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GeminiTranslationServiceImpl implements TranslationService {

    @Value("${gcp.project.id}")
    private String gcpProjectId;

    @Value("${gcp.location}")
    private String gcpLocation;

    @Override
    public String translate(String text, String targetLanguage) {
        try (VertexAI vertexAI = new VertexAI(gcpProjectId, gcpLocation)) {
            GenerativeModel model = new GenerativeModel("gemini-1.5-flash-001", vertexAI);
            String prompt = String.format("Translate the following text to %s: \"%s\"", targetLanguage, text);
            GenerateContentResponse response = model.generateContent(prompt);
            return ResponseHandler.getText(response);
        } catch (IOException e) {
            // Em caso de erro, retorne o texto original
            return text;
        }
    }
}