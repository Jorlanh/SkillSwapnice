package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import br.com.teamss.skillswap.skill_swap.model.entities.CommunityMember;
import br.com.teamss.skillswap.skill_swap.model.entities.CommunityMemberId;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.Like;
import br.com.teamss.skillswap.skill_swap.model.entities.LikeId;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import br.com.teamss.skillswap.skill_swap.model.entities.Repost;
import br.com.teamss.skillswap.skill_swap.model.entities.RepostId;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommunityRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommunityMemberRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.LikeRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.RepostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final ProfileRepository profileRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final RepostRepository repostRepository;
    private final ShareLinkRepository shareLinkRepository;

    @Autowired
    public CommunityServiceImpl(
            CommunityRepository communityRepository,
            UserRepository userRepository,
            PostRepository postRepository,
            CommunityMemberRepository communityMemberRepository,
            ProfileRepository profileRepository,
            LikeRepository likeRepository,
            CommentRepository commentRepository,
            RepostRepository repostRepository,
            ShareLinkRepository shareLinkRepository) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.communityMemberRepository = communityMemberRepository;
        this.profileRepository = profileRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.repostRepository = repostRepository;
        this.shareLinkRepository = shareLinkRepository;
    }

    @Override
    public List<Community> getCommunities(int limit) {
        return communityRepository.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Community joinCommunity(UUID communityId, UUID userId) {
        System.out.println("Iniciando joinCommunity com communityId: " + communityId + ", userId: " + userId);
        joinCommunityWithMemberRepository(communityId, userId);
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
    }

    private CommunityMember joinCommunityWithMemberRepository(UUID communityId, UUID userId) {
        System.out.println("Iniciando joinCommunityWithMemberRepository com communityId: " + communityId + ", userId: " + userId);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidade não encontrada com ID: " + communityId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + userId));

        if (communityMemberRepository.existsById_CommunityIdAndId_UserId(communityId, userId)) {
            throw new IllegalStateException("Usuário já é membro da comunidade com ID: " + communityId);
        }

        CommunityMember communityMember = new CommunityMember();
        CommunityMemberId id = new CommunityMemberId();
        id.setCommunityId(communityId);
        id.setUserId(userId);
        communityMember.setId(id);
        communityMember.setJoinedAt(Instant.now());

        CommunityMember savedMember = communityMemberRepository.save(communityMember);

        community.getMembers().add(user);
        communityRepository.save(community);

        return savedMember;
    }

    private static final String UPLOAD_DIR = "uploads/";
    private static final long MAX_IMAGE_SIZE = 20 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;

    @Override
    public List<Post> getCommunityPosts(UUID communityId) {
        return postRepository.findByCommunity_CommunityId(communityId);
    }

    @Override
    public Post createCommunityPost(UUID communityId, UUID userId, String title, String content, 
                                    MultipartFile image, MultipartFile video) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));

        Profile profile = profileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado para usuário com ID: " + userId));

        if (image != null && !image.isEmpty()) {
            if (image.getSize() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("A imagem excede o limite de 20MB!");
            }
        }
        if (video != null && !video.isEmpty()) {
            if (video.getSize() > MAX_VIDEO_SIZE) {
                throw new IllegalArgumentException("O vídeo excede o limite de 100MB!");
            }
        }

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String imageUrl = null;
        String videoUrl = null;

        if (image != null && !image.isEmpty()) {
            String imageFileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path imagePath = uploadPath.resolve(imageFileName);
            Files.write(imagePath, image.getBytes());
            imageUrl = UPLOAD_DIR + imageFileName;
        }

        if (video != null && !video.isEmpty()) {
            String videoFileName = UUID.randomUUID() + "_" + video.getOriginalFilename();
            Path videoPath = uploadPath.resolve(videoFileName);
            Files.write(videoPath, video.getBytes());
            videoUrl = UPLOAD_DIR + videoFileName;
        }

        Post post = new Post();
        post.setUser(user);
        post.setProfile(profile);
        post.setCommunity(community);
        post.setTitle(title);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setVideoUrl(videoUrl);
        post.setCreatedAt(Instant.now());
        post.setLikesCount(0);
        post.setRepostsCount(0);
        post.setCommentsCount(0);
        post.setSharesCount(0);
        post.setViewsCount(0);

        return postRepository.save(post);
    }

    @Override
    public Like likePost(UUID communityId, Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (likeRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            throw new IllegalStateException("Usuário já curtiu este post!");
        }

        Like like = new Like(post, user);
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);

        return likeRepository.save(like);
    }

    @Override
    public Comment commentPost(UUID communityId, Long postId, CommentDTO commentDTO) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(commentDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(commentDTO.getContent());
        comment.setCreatedAt(Instant.now());

        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return commentRepository.save(comment);
    }

    @Override
    public Repost repostPost(UUID communityId, Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (repostRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) { // CORRIGIDO
            throw new IllegalStateException("Usuário já repostou este post!");
        }

        Repost repost = new Repost(post, user);
        post.setRepostsCount(post.getRepostsCount() + 1);
        postRepository.save(post);

        return repostRepository.save(repost);
    }

    @Override
    public ShareLink sharePost(UUID communityId, Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        String shareUrl = "https://skillswap.com/share/post/" + UUID.randomUUID();

        ShareLink shareLink = new ShareLink();
        shareLink.setPost(post);
        shareLink.setUser(user);
        shareLink.setShareUrl(shareUrl);
        shareLink.setCreatedAt(Instant.now());
        shareLink.setClickCount(0);

        post.setSharesCount(post.getSharesCount() + 1);
        postRepository.save(post);

        return shareLinkRepository.save(shareLink);
    }
}