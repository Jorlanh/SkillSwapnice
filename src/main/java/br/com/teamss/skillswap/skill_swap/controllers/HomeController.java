package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.dto.LikeDTO;
import br.com.teamss.skillswap.skill_swap.dto.NotificationDTO;
import br.com.teamss.skillswap.skill_swap.dto.PostResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.services.CommentService;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.services.PostService;
import br.com.teamss.skillswap.skill_swap.model.services.TrendingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final PostService postService;
    private final CommunityService communityService;
    private final NotificationService notificationService;
    private final ShareLinkRepository shareLinkRepository;
    private final TrendingService trendingService;
    private final CommentService commentService;

    @Autowired
    public HomeController(PostService postService, CommunityService communityService,
                          NotificationService notificationService, ShareLinkRepository shareLinkRepository,
                          TrendingService trendingService, CommentService commentService) {
        this.postService = postService;
        this.communityService = communityService;
        this.notificationService = notificationService;
        this.shareLinkRepository = shareLinkRepository;
        this.trendingService = trendingService;
        this.commentService = commentService;
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponseDTO>> getPosts(
            @RequestParam(defaultValue = "TRENDING") String sortBy,
            @RequestParam(defaultValue = "DAY") String period) {
        Instant startTime;
        switch (period.toUpperCase()) {
            case "DAY":
                startTime = Instant.now().minusSeconds(86400);
                break;
            case "WEEK":
                startTime = Instant.now().minusSeconds(604800);
                break;
            case "MONTH":
                startTime = Instant.now().minusSeconds(2592000);
                break;
            default:
                startTime = Instant.now().minusSeconds(86400);
        }
        List<PostResponseDTO> posts = postService.getPosts(sortBy, startTime);
        posts.forEach(post -> postService.incrementViewCount(post.getPostId()));
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(
            @RequestParam UUID userId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile video) throws IOException {
        
        Post post = postService.createPost(userId, title, content, image, video);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Post> likePost(@PathVariable Long postId, @RequestParam UUID userId) {
        return ResponseEntity.ok(postService.likePost(postId, userId));
    }

    @PostMapping("/posts/{postId}/repost")
    public ResponseEntity<Post> repost(@PathVariable Long postId, @RequestParam UUID userId) {
        return ResponseEntity.ok(postService.repost(postId, userId));
    }

    @PostMapping("/posts/{postId}/comment")
    public ResponseEntity<Post> commentOnPost(
            @PathVariable Long postId,
            @RequestParam UUID userId,
            @RequestParam String content) {
        return ResponseEntity.ok(postService.commentOnPost(postId, userId, content));
    }

    @GetMapping("/posts/{postId}/share")
    public ResponseEntity<String> generateShareLink(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.generateShareLink(postId));
    }

    @GetMapping("/trending-topics")
    public ResponseEntity<List<String>> getTrendingTopics(@RequestParam(defaultValue = "DAY") String period) {
        return ResponseEntity.ok(postService.getTrendingTopics(period));
    }

    @GetMapping("/communities")
    public ResponseEntity<List<Community>> getCommunities(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(communityService.getCommunities(limit));
    }

    @PostMapping("/communities/{communityId}/join")
    public ResponseEntity<Community> joinCommunity(
            @PathVariable UUID communityId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(communityService.joinCommunity(communityId, userId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(@RequestParam UUID userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/share/click")
    public ResponseEntity<Void> trackShareClick(@RequestParam String shareUrl) {
        ShareLink shareLink = shareLinkRepository.findByShareUrl(shareUrl)
                .orElseThrow(() -> new RuntimeException("Link de compartilhamento não encontrado!"));
        shareLink.setClickCount(shareLink.getClickCount() + 1);
        shareLinkRepository.save(shareLink);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/trending-posts")
    public ResponseEntity<List<Post>> getTrendingPosts(@RequestParam(defaultValue = "DAY") String period) {
        List<Post> trendingPosts = trendingService.getTrendingPosts(period, 10);
        return ResponseEntity.ok(trendingPosts);
    }

    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<List<LikeDTO>> getLikesByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getLikesByPost(postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }
}