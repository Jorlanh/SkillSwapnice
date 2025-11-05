package br.com.teamss.skillswap.skill_swap.model.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.teamss.skillswap.skill_swap.dto.SearchResultDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;

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
            .index(indicesToSearch)
            .query(multiMatchQuery);

        // Lógica de ordenação
        List<SortOptions> sortOptions = new ArrayList<>();
        if ("DATE".equalsIgnoreCase(sortBy)) {
            // Ordena por data (mais recentes primeiro)
            sortOptions.add(SortOptions.of(s -> s.field(f -> f
                .field("createdAt")
                .order(SortOrder.Desc)
                .unmappedType(FieldType.Long)
                .missing(FieldValue.of("_last")) // CORRIGIDO
            )));
            // Desempate por relevância (score)
            sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
        } else {
            // Padrão: relevância
            sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
        }

        // Desempate final por ID
        sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("_id").order(SortOrder.Asc))));
        requestBuilder.sort(sortOptions);

        // Executa a busca
        SearchRequest searchRequest = requestBuilder.build();
        SearchResponse<JsonNode> response = client.search(searchRequest, JsonNode.class);

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

        switch (type) {
            case "user":
                title = source.has("username") ? source.get("username").asText() : "";
                description = source.has("bio") ? source.get("bio").asText() : "";
                imageUrl = source.has("imageUrl") ? source.get("imageUrl").asText() : "";
                break;
            case "post":
                title = source.has("title") ? source.get("title").asText() : "";
                description = source.has("content") ? source.get("content").asText() : "";
                imageUrl = source.has("imageUrl") ? source.get("imageUrl").asText() : "";
                break;
            case "community":
                title = source.has("name") ? source.get("name").asText() : "";
                description = source.has("description") ? source.get("description").asText() : "";
                break;
        }

        // Corrigido: evitar auto-unboxing nulo
        Double scoreObj = hit.score();
        double score = (scoreObj != null) ? scoreObj.doubleValue() : 0.0;

        return new SearchResultDTO(id, type, title, description, imageUrl, score);
    }

    private String mapIndexToType(String index) {
        if (USER_INDEX.equals(index)) return "user";
        if (POST_INDEX.equals(index)) return "post";
        if (COMMUNITY_INDEX.equals(index)) return "community";
        return "unknown";
    }

    // --- Métodos de sincronização ---

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