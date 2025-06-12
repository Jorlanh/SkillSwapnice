package br.com.teamss.skillswap.skill_swap.model.services;

import java.util.List;

public interface SearchService {
    List<Object> search(String query, String filter, String sortBy);
}