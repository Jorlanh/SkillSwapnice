package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserIdDTO;
import br.com.teamss.skillswap.skill_swap.dto.PostDTO;
import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.dto.ShareLinkDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Like;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import br.com.teamss.skillswap.skill_swap.model.entities.Repost;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @PostMapping("/{communityId}/join")
    public ResponseEntity<Community> joinCommunity(
            @PathVariable UUID communityId,
            @Valid @RequestBody UserIdDTO userIdDTO) {
        System.out.println("Recebido no controlador: communityId=" + communityId + ", userId=" + userIdDTO.getUserId());
        Community community = communityService.joinCommunity(communityId, userIdDTO.getUserId());
        return ResponseEntity.ok(community);
    }

    @GetMapping("/{communityId}/posts")
    public ResponseEntity<List<PostDTO>> getCommunityPosts(@PathVariable UUID communityId) {
        List<Post> posts = communityService.getCommunityPosts(communityId);
        List<PostDTO> postDTOs = posts.stream().map(post -> {
            PostDTO dto = new PostDTO();
            dto.setPostId(post.getPostId());
            dto.setContent(post.getContent());
            dto.setTitle(post.getTitle());
            dto.setUserId(post.getUser().getUserId());
            dto.setUsername(post.getUser().getUsername());
            dto.setProfileId(post.getProfile().getProfileId());
            dto.setCommunityId(post.getCommunity().getCommunityId());
            dto.setCommunityName(post.getCommunity().getName());
            dto.setImageUrl(post.getImageUrl());
            dto.setVideoUrl(post.getVideoUrl());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setLikesCount(post.getLikesCount());
            dto.setRepostsCount(post.getRepostsCount());
            dto.setCommentsCount(post.getCommentsCount());
            dto.setSharesCount(post.getSharesCount());
            dto.setViewsCount(post.getViewsCount());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(postDTOs);
    }

    @PostMapping("/{communityId}/posts")
    public ResponseEntity<PostDTO> createCommunityPost(
            @PathVariable UUID communityId,
            @RequestParam("userId") UUID userId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video) throws IOException {
        Post post = communityService.createCommunityPost(communityId, userId, title, content, image, video);
        PostDTO dto = new PostDTO();
        dto.setPostId(post.getPostId());
        dto.setContent(post.getContent());
        dto.setTitle(post.getTitle());
        dto.setUserId(post.getUser().getUserId());
        dto.setUsername(post.getUser().getUsername());
        dto.setProfileId(post.getProfile().getProfileId());
        dto.setCommunityId(post.getCommunity().getCommunityId());
        dto.setCommunityName(post.getCommunity().getName());
        dto.setImageUrl(post.getImageUrl());
        dto.setVideoUrl(post.getVideoUrl());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setLikesCount(post.getLikesCount());
        dto.setRepostsCount(post.getRepostsCount());
        dto.setCommentsCount(post.getCommentsCount());
        dto.setSharesCount(post.getSharesCount());
        dto.setViewsCount(post.getViewsCount());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{communityId}/posts/{postId}/like")
    public ResponseEntity<Like> likePost(
            @PathVariable UUID communityId,
            @PathVariable Long postId,
            @Valid @RequestBody UserIdDTO userIdDTO) {
        Like like = communityService.likePost(communityId, postId, userIdDTO.getUserId());
        return ResponseEntity.ok(like);
    }

    @PostMapping("/{communityId}/posts/{postId}/comment")
    public ResponseEntity<CommentDTO> commentPost(
            @PathVariable UUID communityId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentDTO commentDTO) {
        Comment comment = communityService.commentPost(communityId, postId, commentDTO);
        CommentDTO dto = new CommentDTO();
        dto.setCommentId(comment.getCommentId());
        dto.setPostId(comment.getPost().getPostId());
        dto.setUserId(comment.getUser().getUserId());
        dto.setUsername(comment.getUser().getUsername());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{communityId}/posts/{postId}/repost")
    public ResponseEntity<Repost> repostPost(
            @PathVariable UUID communityId,
            @PathVariable Long postId,
            @Valid @RequestBody UserIdDTO userIdDTO) {
        Repost repost = communityService.repostPost(communityId, postId, userIdDTO.getUserId());
        return ResponseEntity.ok(repost);
    }

    @PostMapping("/{communityId}/posts/{postId}/share")
    public ResponseEntity<ShareLinkDTO> sharePost(
            @PathVariable UUID communityId,
            @PathVariable Long postId,
            @Valid @RequestBody UserIdDTO userIdDTO) {
        ShareLink shareLink = communityService.sharePost(communityId, postId, userIdDTO.getUserId());
        ShareLinkDTO dto = new ShareLinkDTO();
        dto.setShareId(shareLink.getShareId());
        dto.setPostId(shareLink.getPost().getPostId());
        dto.setUserId(shareLink.getUser().getUserId());
        dto.setShareUrl(shareLink.getShareUrl());
        dto.setCreatedAt(shareLink.getCreatedAt());
        dto.setClickCount(shareLink.getClickCount());
        return ResponseEntity.ok(dto);
    }
}