package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class GeminiApiModerationService implements ContentModerationService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gcp.project.id}")
    private String gcpProjectId;

    private static final String GEMINI_MODEL = "gemini-1.5-flash-001";
    private static final String GCP_LOCATION = "us-central1";

    @Override
    public boolean isContentInappropriate(String text) {
        if (apiKey == null || apiKey.isBlank() || gcpProjectId == null || gcpProjectId.isBlank()) {
            System.err.println("AVISO: Credenciais da API Gemini ou ID do Projeto não configurados. Moderação desativada.");
            return false;
        }

        if (text == null || text.isBlank()) {
            return false;
        }

        try (VertexAI vertexAI = new VertexAI(gcpProjectId, GCP_LOCATION)) {
            GenerativeModel model = new GenerativeModel(GEMINI_MODEL, vertexAI);

            String prompt = String.format(
                "Você é um moderador de conteúdo para uma plataforma social chamada SkillSwap. " +
                "Sua tarefa é analisar o texto a seguir e determinar se ele viola alguma das seguintes categorias: " +
                "discurso de ódio, assédio, violência explícita, conteúdo sexualmente explícito, ameaças, spam, ou incentivo à automutilação. " +
                "Responda apenas com 'SIM' se violar alguma categoria, ou 'NÃO' se for seguro. Não dê nenhuma outra explicação. " +
                "Texto para analisar: \"%s\"", text
            );
            
            // **MELHORIA APLICADA AQUI**
            // Ao omitir o parâmetro "languages", a API irá detetar o idioma automaticamente.
            GenerateContentResponse response = model.generateContent(prompt);
            String modelResponse = ResponseHandler.getText(response).trim();

            return "SIM".equalsIgnoreCase(modelResponse);

        } catch (IOException e) {
            System.err.println("Erro ao comunicar com a API Vertex AI (Gemini): " + e.getMessage());
            return false;
        }
    }
}