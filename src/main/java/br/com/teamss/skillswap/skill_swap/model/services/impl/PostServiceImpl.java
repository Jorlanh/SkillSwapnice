package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.*;
import br.com.teamss.skillswap.skill_swap.model.repositories.*;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository, 
                           NotificationService notificationService, LikeRepository likeRepository,
                           RepostRepository repostRepository, ShareLinkRepository shareLinkRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.likeRepository = likeRepository;
        this.repostRepository = repostRepository;
        this.shareLinkRepository = shareLinkRepository;
    }

    @Override
    public Post createPost(UUID userId, String title, String content, String imageUrl, String videoUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setProfile(user.getProfile());
        // post.setImageUrl(imageUrl);
        // post.setVideoUrl(videoUrl);
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

        if (!repostRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) { // CORRIGIDO
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
    public Post commentOnPost(Long postId, UUID userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));

        post.setCommentsCount(post.getCommentsCount() + 1);
        post = postRepository.save(post);

        notificationService.createNotification(post.getUser().getUserId(),
                "Seu post recebeu um comentário de " + userRepository.findById(userId).get().getUsername());

        return post;
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
    public List<Post> getPosts(String sortBy, Instant startTime) {
        List<Post> posts = postRepository.findTrendingPosts(startTime);
        switch (sortBy.toUpperCase()) {
            case "LIKES":
                return posts.stream()
                        .sorted(Comparator.comparingInt(Post::getLikesCount).reversed())
                        .collect(Collectors.toList());
            case "REPOSTS":
                return posts.stream()
                        .sorted(Comparator.comparingInt(Post::getRepostsCount).reversed())
                        .collect(Collectors.toList());
            case "COMMENTS":
                return posts.stream()
                        .sorted(Comparator.comparingInt(Post::getCommentsCount).reversed())
                        .collect(Collectors.toList());
            case "SHARES":
                return posts.stream()
                        .sorted(Comparator.comparingInt(Post::getSharesCount).reversed())
                        .collect(Collectors.toList());
            case "DATE":
                return posts.stream()
                        .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                        .collect(Collectors.toList());
            case "TRENDING":
            default:
                return posts;
        }
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
}