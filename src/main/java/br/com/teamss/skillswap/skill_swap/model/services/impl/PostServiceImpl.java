package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.LikeDTO;
import br.com.teamss.skillswap.skill_swap.dto.PostResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.*;
import br.com.teamss.skillswap.skill_swap.model.repositories.*;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LikeRepository likeRepository;
    private final RepostRepository repostRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final CommentRepository commentRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository,
                           NotificationService notificationService, LikeRepository likeRepository,
                           RepostRepository repostRepository, ShareLinkRepository shareLinkRepository,
                           CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.likeRepository = likeRepository;
        this.repostRepository = repostRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public Post createPost(UUID userId, String title, String content, MultipartFile image, MultipartFile video) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        String imageUrl = fileUploadService.uploadFile(image);
        String videoUrl = fileUploadService.uploadFile(video);

        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setProfile(user.getProfile());
        post.setImageUrl(imageUrl);
        post.setVideoUrl(videoUrl);
        post.setLikesCount(0);
        post.setRepostsCount(0);
        post.setCommentsCount(0);
        post.setSharesCount(0);
        post.setCreatedAt(Instant.now());

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post likePost(Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (!likeRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            Like like = new Like(post, user);
            likeRepository.save(like);
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);

            notificationService.createNotification(post.getUser().getUserId(),
                    "Seu post foi curtido por " + user.getUsername());
        }

        return post;
    }

    @Override
    @Transactional
    public Post repost(Long postId, UUID userId) {
        Post originalPost = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (!repostRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            Repost repost = new Repost(originalPost, user);
            repostRepository.save(repost);
            originalPost.setRepostsCount(originalPost.getRepostsCount() + 1);
            postRepository.save(originalPost);

            notificationService.createNotification(originalPost.getUser().getUserId(),
                    "Seu post foi repostado por " + user.getUsername());
        }

        Post repostPost = new Post();
        repostPost.setUser(user);
        repostPost.setTitle("Repost: " + originalPost.getTitle());
        repostPost.setContent(originalPost.getContent());
        repostPost.setProfile(user.getProfile());
        repostPost.setLikesCount(0);
        repostPost.setRepostsCount(0);
        repostPost.setCommentsCount(0);
        repostPost.setSharesCount(0);
        postRepository.save(repostPost);

        return originalPost;
    }

    @Override
    @Transactional
    public Post commentOnPost(Long postId, UUID userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        
        Comment newComment = new Comment();
        newComment.setPost(post);
        newComment.setUser(user);
        newComment.setContent(content);
        commentRepository.save(newComment);

        post.setCommentsCount(post.getCommentsCount() + 1);
        Post updatedPost = postRepository.save(post);

        notificationService.createNotification(post.getUser().getUserId(),
                "Seu post recebeu um comentário de " + user.getUsername());

        return updatedPost;
    }

    @Override
    @Transactional
    public String generateShareLink(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = post.getUser();

        String shareUrl = "https://skillswap.com/share/post/" + UUID.randomUUID().toString();
        ShareLink shareLink = new ShareLink();
        shareLink.setPost(post);
        shareLink.setUser(user);
        shareLink.setShareUrl(shareUrl);
        shareLink.setClickCount(0);

        shareLinkRepository.save(shareLink);
        post.setSharesCount(post.getSharesCount() + 1);
        postRepository.save(post);

        return shareUrl;
    }

    @Override
    public List<PostResponseDTO> getPosts(String sortBy, Instant startTime) {
        List<Post> posts = postRepository.findTrendingPosts(startTime);
        
        // A lógica de ordenação precisa ser aplicada antes da conversão para DTO
        switch (sortBy.toUpperCase()) {
            case "LIKES":
                posts.sort(Comparator.comparingInt(Post::getLikesCount).reversed());
                break;
            case "REPOSTS":
                posts.sort(Comparator.comparingInt(Post::getRepostsCount).reversed());
                break;
            case "COMMENTS":
                posts.sort(Comparator.comparingInt(Post::getCommentsCount).reversed());
                break;
            case "SHARES":
                posts.sort(Comparator.comparingInt(Post::getSharesCount).reversed());
                break;
            case "DATE":
                posts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
                break;
            case "TRENDING":
            default:
                // A query já retorna ordenado por trending
                break;
        }

        // Converte a lista de Post para PostResponseDTO
        return posts.stream().map(post -> {
            PostResponseDTO dto = new PostResponseDTO();
            UserSummaryDTO userSummary = new UserSummaryDTO(
                post.getUser().getUserId(),
                post.getUser().getUsername(),
                post.getUser().getName()
            );
            dto.setPostId(post.getPostId());
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setUser(userSummary);
            dto.setImageUrl(post.getImageUrl());
            dto.setVideoUrl(post.getVideoUrl());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setLikesCount(post.getLikesCount());
            dto.setCommentsCount(post.getCommentsCount());
            dto.setRepostsCount(post.getRepostsCount());
            dto.setSharesCount(post.getSharesCount());
            dto.setViewsCount(post.getViewsCount());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getTrendingTopics(String period) {
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

        List<Post> posts = postRepository.findTrendingPosts(startTime);
        Map<String, Double> topicScores = new HashMap<>();

        for (Post post : posts) {
            String[] words = post.getContent().toLowerCase().split("\\W+");
            for (String word : words) {
                if (word.length() > 3) {
                    double score = (post.getLikesCount() + post.getRepostsCount() +
                            post.getCommentsCount() + post.getSharesCount()) / 4.0;
                    topicScores.merge(word, score, Double::sum);
                }
            }
        }

        return topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);
    }
    
    @Override
    public List<LikeDTO> getLikesByPost(Long postId) {
        List<Like> likes = likeRepository.findAllByPost_PostId(postId);
        return likes.stream()
                .map(like -> {
                    User user = like.getUser();
                    UserSummaryDTO userSummary = new UserSummaryDTO(user.getUserId(), user.getUsername(), user.getName());
                    return new LikeDTO(userSummary, like.getCreatedAt());
                })
                .collect(Collectors.toList());
    }
}