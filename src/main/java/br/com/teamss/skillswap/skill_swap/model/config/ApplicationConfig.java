package br.com.teamss.skillswap.skill_swap.model.config;

import br.com.teamss.skillswap.skill_swap.model.repositories.CommentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommunityMemberRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommunityRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.LikeRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.RepostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.ProfileService;
import br.com.teamss.skillswap.skill_swap.model.services.TrendingService;
import br.com.teamss.skillswap.skill_swap.model.services.impl.CommunityServiceImpl;
import br.com.teamss.skillswap.skill_swap.model.services.impl.ProfileServiceImpl;
import br.com.teamss.skillswap.skill_swap.model.services.impl.TrendingServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public TrendingService trendingService(PostRepository postRepository, ShareLinkRepository shareLinkRepository) {
        return new TrendingServiceImpl(postRepository, shareLinkRepository);
    }

    @Bean
    public ProfileService profileService(UserRepository userRepository, PostRepository postRepository) {
        return new ProfileServiceImpl(userRepository, postRepository);
    }

    @Bean
    public CommunityService communityService(CommunityRepository communityRepository, UserRepository userRepository,
            PostRepository postRepository, CommunityMemberRepository communityMemberRepository,
            ProfileRepository profileRepository, LikeRepository likeRepository, CommentRepository commentRepository,
            RepostRepository repostRepository, ShareLinkRepository shareLinkRepository,
            FileUploadService fileUploadService, ContentModerationService moderationService) {
        return new CommunityServiceImpl(communityRepository, userRepository, postRepository, communityMemberRepository,
                profileRepository, likeRepository, commentRepository, repostRepository, shareLinkRepository,
                fileUploadService, moderationService);
    }
}