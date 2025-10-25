package br.com.teamss.skillswap.skill_swap.controllers;

import java.util.List;
import java.util.UUID;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.ProfileService;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final UserServiceDTO userServiceDTO;

    public ProfileController(ProfileService profileService, UserServiceDTO userServiceDTO) {
        this.profileService = profileService;
        this.userServiceDTO = userServiceDTO;
    }

    // Endpoint público para ver um perfil por ID de utilizador.
    @GetMapping("/user/{userId}")
    public ResponseEntity<Profile> getProfileByUserId(@PathVariable UUID userId) {
        Profile profile = profileService.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found"));
        return ResponseEntity.ok(profile);
    }

    // Endpoint seguro para o utilizador autenticado atualizar o seu próprio perfil.
    @PutMapping("/me")
    public ResponseEntity<Profile> updateMyProfile(@RequestBody Profile profileDetails) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Profile userProfile = profileService.findByUserId(authenticatedUser.getUserId())
            .orElseThrow(() -> new RuntimeException("Profile not found for authenticated user"));
        Profile updatedProfile = profileService.update(userProfile.getProfileId(), profileDetails);
        return ResponseEntity.ok(updatedProfile);
    }

    // Endpoint seguro para obter o feed do utilizador autenticado.
    @GetMapping("/me/feed")
    public ResponseEntity<List<Post>> getMyFeed() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(profileService.getFeed(authenticatedUser.getUserId()));
    }

    // Endpoint seguro para o utilizador autenticado interagir com publicações.
    @PostMapping("/me/posts/{postId}/like")
    public ResponseEntity<Post> likePost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(profileService.likePost(postId, authenticatedUser.getUserId()));
    }

    @PostMapping("/me/posts/{postId}/comment")
    public ResponseEntity<Post> commentOnPost(@PathVariable Long postId, @RequestParam String content) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(profileService.commentOnPost(postId, authenticatedUser.getUserId(), content));
    }
    
    // Endpoint seguro para o utilizador autenticado seguir outro utilizador.
    @PostMapping("/me/follow/{targetUserId}")
    public ResponseEntity<Void> followUser(@PathVariable UUID targetUserId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        profileService.followUser(authenticatedUser.getUserId(), targetUserId);
        return ResponseEntity.ok().build();
    }

    // Endpoint seguro para o utilizador autenticado deixar de seguir outro utilizador.
    @PostMapping("/me/unfollow/{targetUserId}")
    public ResponseEntity<Void> unfollowUser(@PathVariable UUID targetUserId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        profileService.unfollowUser(authenticatedUser.getUserId(), targetUserId);
        return ResponseEntity.ok().build();
    }
}