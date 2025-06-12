package br.com.teamss.skillswap.skill_swap.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.ProfileService;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<List<Profile>> getAllProfiles() {
        List<Profile> profiles = profileService.findAll();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Profile> getProfileById(@PathVariable Long id) {
        Profile profile = profileService.findById(id).orElse(null);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Profile> getProfileByUserId(@PathVariable UUID userId) {
        Profile profile = profileService.findByUserId(userId).orElse(null);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Profile> createProfile(@RequestBody Profile profile, @PathVariable UUID userId) {
        Profile createdProfile = profileService.createProfile(profile, userId);
        return ResponseEntity.ok(createdProfile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Profile> updateProfile(@PathVariable Long id, @RequestBody Profile profileDetails) {
        Profile updatedProfile = profileService.update(id, profileDetails);
        return ResponseEntity.ok(updatedProfile);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/feed")
    public ResponseEntity<List<Post>> getFeed(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getFeed(userId));
    }

    @PostMapping("/{userId}/posts/{postId}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable UUID userId, @PathVariable Long postId) {
        profileService.incrementViewCount(postId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/posts/{postId}/like")
    public ResponseEntity<Post> likePost(@PathVariable UUID userId, @PathVariable Long postId) {
        return ResponseEntity.ok(profileService.likePost(postId, userId));
    }

    @PostMapping("/{userId}/posts/{postId}/comment")
    public ResponseEntity<Post> commentOnPost(@PathVariable UUID userId, @PathVariable Long postId, @RequestParam String content) {
        return ResponseEntity.ok(profileService.commentOnPost(postId, userId, content));
    }

    @PostMapping("/{userId}/posts/{postId}/repost")
    public ResponseEntity<Post> repost(@PathVariable UUID userId, @PathVariable Long postId) {
        return ResponseEntity.ok(profileService.repost(postId, userId));
    }

    @GetMapping("/{userId}/posts/{postId}/share")
    public ResponseEntity<String> generateShareLink(@PathVariable UUID userId, @PathVariable Long postId) {
        return ResponseEntity.ok(profileService.generateShareLink(postId));
    }

    @GetMapping("/{userId}/achievements")
    public ResponseEntity<List<String>> getAchievements(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getAchievements(userId));
    }

    @GetMapping("/{userId}/activity")
    public ResponseEntity<List<Post>> getActivity(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getActivity(userId));
    }

    @GetMapping("/{userId}/highlights")
    public ResponseEntity<List<Post>> getHighlights(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getHighlights(userId));
    }

    @GetMapping("/{userId}/communities")
    public ResponseEntity<List<Post>> getCommunityPosts(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getCommunityPosts(userId));
    }

    @PostMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<Void> followUser(@PathVariable UUID userId, @PathVariable UUID targetUserId) {
        profileService.followUser(userId, targetUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/unfollow/{targetUserId}")
    public ResponseEntity<Void> unfollowUser(@PathVariable UUID userId, @PathVariable UUID targetUserId) {
        profileService.unfollowUser(userId, targetUserId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/edit")
    public ResponseEntity<User> updateProfile(@PathVariable UUID userId, @RequestBody User updatedUser) {
        return ResponseEntity.ok(profileService.updateProfile(userId, updatedUser));
    }

    @GetMapping("/{userId}/edit/back")
    public ResponseEntity<Void> backToProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{senderId}/messages/{receiverId}")
    public ResponseEntity<Void> sendMessage(@PathVariable UUID senderId, @PathVariable UUID receiverId, @RequestParam String content, @RequestParam String type) {
        profileService.sendMessage(senderId, receiverId, content, type);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/schedule")
    public ResponseEntity<Void> scheduleLesson(@PathVariable UUID userId, @RequestParam String calendarEvent) {
        profileService.scheduleLesson(userId, calendarEvent);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/messages")
    public ResponseEntity<List<UUID>> getMessages(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getMessages(userId));
    }
}