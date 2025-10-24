package br.com.teamss.skillswap.skill_swap.model.config;

import br.com.teamss.skillswap.skill_swap.controllers.HomeController;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.services.PostService;
import br.com.teamss.skillswap.skill_swap.model.services.TrendingService;
import br.com.teamss.skillswap.skill_swap.model.services.CommentService; // 1. Importe o CommentService
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class HomeControllerConfig {

    @Bean
    @Primary
    public HomeController homeController(PostService postService, CommunityService communityService,
                                         NotificationService notificationService, ShareLinkRepository shareLinkRepository,
                                         TrendingService trendingService, CommentService commentService) { // 2. Adicione o CommentService como parâmetro
        return new HomeController(postService, communityService, notificationService, shareLinkRepository, trendingService, commentService); // 3. Passe o commentService para o construtor
    }
}