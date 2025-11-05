package br.com.teamss.skillswap.skill_swap.dto;

public record SearchResultDTO(
    String id,
    String type, // "user", "post", "community"
    String title, // username, post title, community name
    String description,
    String imageUrl,
    double score
) {}