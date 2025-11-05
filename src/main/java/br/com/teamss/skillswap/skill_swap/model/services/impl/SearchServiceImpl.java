package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.SearchResultDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;
import com.fasterxml.jackson.databind.JsonNode;
// ADICIONADO: IMPORTS DO OPENSEARCH
import org.opensearch.client.java.OpenSearchClient;
import org.opensearch.client.java.core.DeleteRequest;
import org.opensearch.client.java.core.IndexRequest;
import org.opensearch.client.java.core.SearchRequest;
import org.opensearch.client.java.core.search.Hit;
import org.opensearch.client.java.core.search.TotalHits;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;

// **** IMPORT ADICIONADO PARA CORRIGIR O ERRO ****
import org.opensearch.client.opensearch._types.mapping.FieldType; 

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final OpenSearchClient client;

    // Índices no OpenSearch
    public static final String USER_INDEX = "users";
    public static final String POST_INDEX = "posts";
    public static final String COMMUNITY_INDEX = "communities";

    public SearchServiceImpl(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public List<SearchResultDTO> search(String query, String filter, String sortBy) throws IOException {
        List<String> indicesToSearch = new ArrayList<>();
        if ("ALL".equalsIgnoreCase(filter)) {
            indicesToSearch.add(USER_INDEX);
            indicesToSearch.add(POST_INDEX);
            indicesToSearch.add(COMMUNITY_INDEX);
        } else if ("PROFILE".equalsIgnoreCase(filter)) {
            indicesToSearch.add(USER_INDEX);
        } else if ("CONTENT".equalsIgnoreCase(filter)) {
            indicesToSearch.add(POST_INDEX);
        } else if ("COMMUNITY".equalsIgnoreCase(filter)) {
            indicesToSearch.add(COMMUNITY_INDEX);
        }

        // Constrói a query Multi-Match
        Query multiMatchQuery = Query.of(q -> q
            .multiMatch(mm -> mm
                .query(query)
                .fields("username^3", "name^2", "bio", "title^3", "content", "description") // Prioriza campos
                .fuzziness("AUTO")
            )
        );

        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
            .index(indicesToSearch) // CORREÇÃO: .index() em vez de .indices()
            .query(multiMatchQuery);

        // **LÓGICA DE ORDENAÇÃO COMPLETA E CORRIGIDA**
        List<SortOptions> sortOptions = new ArrayList<>();
        if ("DATE".equalsIgnoreCase(sortBy)) {
            // Sorteio principal por data (mais recente primeiro)
            // CORREÇÃO: Encapsula a lambda em SortOptions.of()
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createdAt") // Campo de data
                                             .order(SortOrder.Desc)
                                             
                                             // **** ESTA É A LINHA CORRIGIDA ****
                                             .unmappedType(FieldType.Long) // Trata docs sem 'createdAt' (ex: 'users')
                                             
                                             .missing("_last") // Coloca valores nulos/ausentes no final
            )));
            // Desempate por relevância (score)
            sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
            
        } else { // Padrão é "RELEVANCE"
            // Sorteio principal por relevância (score)
            sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
        }
        // Desempate final por ID para garantir ordem consistente em todos os casos
        sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("_id").order(SortOrder.Asc))));
        
        requestBuilder.sort(sortOptions);
        // **FIM DA LÓGICA DE ORDENAÇÃO**

        SearchRequest searchRequest = requestBuilder.build();
        var response = client.search(searchRequest, JsonNode.class);

        TotalHits total = response.hits().total();
        if (total == null || total.value() == 0) {
            return new ArrayList<>();
        }

        return response.hits().hits().stream()
            .map(this::convertHitToDTO)
            .collect(Collectors.toList());
    }

    private SearchResultDTO convertHitToDTO(Hit<JsonNode> hit) {
        String type = mapIndexToType(hit.index());
        JsonNode source = hit.source();
        if (source == null) {
            return null;
        }

        String id = hit.id();
        String title = "";
        String description = "";
        String imageUrl = "";

        if ("user".equals(type)) {
            title = source.has("username") ? source.get("username").asText() : "";
            description = source.has("bio") ? source.get("bio").asText() : "";
            imageUrl = source.has("imageUrl") ? source.get("imageUrl").asText() : ""; 
        } else if ("post".equals(type)) {
            title = source.has("title") ? source.get("title").asText() : "";
            description = source.has("content") ? source.get("content").asText() : "";
            imageUrl = source.has("imageUrl") ? source.get("imageUrl").asText() : "";
        } else if ("community".equals(type)) {
            title = source.has("name") ? source.get("name").asText() : "";
            description = source.has("description") ? source.get("description").asText() : "";
        }

        // CORREÇÃO: Trata score nulo
        double score = (hit.score() != null) ? hit.score() : 0.0;
        return new SearchResultDTO(id, type, title, description, imageUrl, score);
    }

    private String mapIndexToType(String index) {
        if (USER_INDEX.equals(index)) return "user";
        if (POST_INDEX.equals(index)) return "post";
        if (COMMUNITY_INDEX.equals(index)) return "community";
        return "unknown";
    }

    // --- MÉTODOS DE SINCRONIZAÇÃO (CHAMADOS PELA ETAPA 3) ---

    @Override
    public <T> void indexDocument(String indexName, String docId, T document) throws IOException {
        IndexRequest<T> request = new IndexRequest.Builder<T>()
            .index(indexName)
            .id(docId)
            .document(document)
            .build();
        client.index(request);
    }

    @Override
    public void deleteDocument(String indexName, String docId) throws IOException {
        DeleteRequest request = new DeleteRequest.Builder()
            .index(indexName)
            .id(docId)
            .build();
        client.delete(request);
    }
}