package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileService {
    List<Profile> findAll();
    Optional<Profile> findById(Long id);
    Optional<Profile> findByUserId(UUID userId);
    Profile createProfile(Profile profile, UUID userId);
    Profile save(Profile profile);
    Profile update(Long id, Profile profileDetails);
    void delete(Long id);

    List<Post> getFeed(UUID userId);
    void incrementViewCount(Long postId);
    Post likePost(Long postId, UUID userId);
    Post commentOnPost(Long postId, UUID userId, String content);
    Post repost(Long postId, UUID userId);
    String generateShareLink(Long postId);
    List<Post> getCommunityPosts(UUID userId);
    List<String> getAchievements(UUID userId);
    List<Post> getActivity(UUID userId);
    List<Post> getHighlights(UUID userId);
    void followUser(UUID userId, UUID targetUserId);
    void unfollowUser(UUID userId, UUID targetUserId);
    User updateProfile(UUID userId, User updatedUser);
    void sendMessage(UUID senderId, UUID receiverId, String content, String type); // Tipo: text, image, video, pdf, etc.
    void scheduleLesson(UUID userId, String calendarEvent);
    List<UUID> getMessages(UUID userId);
}