package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PostService {
    Post createPost(UUID userId, String title, String content, String imageUrl, String videoUrl);
    Post likePost(Long postId, UUID userId);
    Post repost(Long postId, UUID userId);
    Post commentOnPost(Long postId, UUID userId, String content);
    String generateShareLink(Long postId);
    List<Post> getPosts(String sortBy, Instant startTime);
    List<String> getTrendingTopics(String period); // "DAY", "WEEK", "MONTH"

    void incrementViewCount(Long postId);
}