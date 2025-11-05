package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.SearchResultDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    // CORREÇÃO: Removido @Autowired (desnecessário em construtor único)
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    // CORREÇÃO: Trocado List<Object> por List<SearchResultDTO> e adicionado throws IOException
    public ResponseEntity<List<SearchResultDTO>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "RELEVANCE") String sortBy) throws IOException {
        return ResponseEntity.ok(searchService.search(query, filter, sortBy));
    }
}