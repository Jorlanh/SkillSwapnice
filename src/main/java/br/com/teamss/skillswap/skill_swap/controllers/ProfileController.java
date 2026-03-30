package br.com.teamss.skillswap.skill_swap.controllers;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserService;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.services.ProfileService;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final UserServiceDTO userServiceDTO;
    private final UserService userService;
    private final FileUploadService fileUploadService;

    public ProfileController(ProfileService profileService, UserServiceDTO userServiceDTO, 
                             UserService userService, FileUploadService fileUploadService) {
        this.profileService = profileService;
        this.userServiceDTO = userServiceDTO;
        this.userService = userService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Profile> getProfileByUserId(@PathVariable UUID userId) {
        Profile profile = profileService.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found"));
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody Map<String, Object> updateRequest) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        
        Profile userProfile = profileService.findByUserId(authenticatedUser.getUserId())
            .orElseThrow(() -> new RuntimeException("Profile not found"));
            
        if (updateRequest.containsKey("description")) {
            userProfile.setDescription((String) updateRequest.get("description"));
        }
        if (updateRequest.containsKey("location")) {
            userProfile.setLocation((String) updateRequest.get("location"));
        }
        
        profileService.update(userProfile.getProfileId(), userProfile);

        String name = (String) updateRequest.get("name");
        String username = (String) updateRequest.get("username");
        
        // Tratamento seguro de cast para evitar o erro de Type Safety
        List<String> skills = null;
        Object skillsObj = updateRequest.get("skills");
        if (skillsObj instanceof List<?>) {
            skills = ((List<?>) skillsObj).stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
        }
        
        if (name != null || username != null || skills != null) {
            userService.updateUserIdentityAndSkills(authenticatedUser.getUserId(), name, username, skills);
        }

        return ResponseEntity.ok().body(Map.of("message", "Perfil atualizado no banco de dados com sucesso."));
    }

    @PatchMapping("/me/privacy")
    public ResponseEntity<?> togglePrivacy(@RequestParam boolean isPrivate) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Profile userProfile = profileService.findByUserId(authenticatedUser.getUserId())
            .orElseThrow(() -> new RuntimeException("Profile not found"));
            
        userProfile.setIsPrivate(isPrivate);
        profileService.update(userProfile.getProfileId(), userProfile);
        
        return ResponseEntity.ok().body(Map.of("message", "Privacidade atualizada para: " + isPrivate));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<?> updateMyAvatar(@RequestParam("file") MultipartFile file) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        String avatarUrl = fileUploadService.uploadFile(file);
        userService.updateAvatar(authenticatedUser.getUserId(), avatarUrl);
        
        return ResponseEntity.ok().body(Map.of(
            "message", "Avatar atualizado com sucesso.",
            "avatarUrl", avatarUrl
        ));
    }

    @GetMapping("/me/feed")
    public ResponseEntity<List<Post>> getMyFeed() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(profileService.getFeed(authenticatedUser.getUserId()));
    }

    @PostMapping("/me/posts")
    public ResponseEntity<?> createPost(@RequestParam("content") String content, 
                                        @RequestParam(value="image", required=false) MultipartFile image) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        profileService.createPost(authenticatedUser.getUserId(), content, image);
        return ResponseEntity.ok().body(Map.of("message", "Post criado e salvo no banco!"));
    }

    @DeleteMapping("/me/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        profileService.deletePost(postId, authenticatedUser.getUserId());
        return ResponseEntity.ok().body(Map.of("message", "Post deletado do banco de dados."));
    }

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

    @PostMapping("/me/follow/{targetUserId}")
    public ResponseEntity<Void> followUser(@PathVariable UUID targetUserId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        profileService.followUser(authenticatedUser.getUserId(), targetUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/unfollow/{targetUserId}")
    public ResponseEntity<Void> unfollowUser(@PathVariable UUID targetUserId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        profileService.unfollowUser(authenticatedUser.getUserId(), targetUserId);
        return ResponseEntity.ok().build();
    }
}