package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.SearchResultDTO;
import java.io.IOException;
import java.util.List;

public interface SearchService {
    
    /**
     * Realiza uma busca full-text nos índices do OpenSearch.
     */
    List<SearchResultDTO> search(String query, String filter, String sortBy) throws IOException;
    
    /**
     * Adiciona ou atualiza um documento em um índice do OpenSearch.
     * (Será chamado de forma assíncrona pelos Listeners).
     */
    <T> void indexDocument(String indexName, String docId, T document) throws IOException;

    /**
     * Remove um documento de um índice do OpenSearch.
     * (Será chamado de forma assíncrona pelos Listeners).
     */
    void deleteDocument(String indexName, String docId) throws IOException;
}