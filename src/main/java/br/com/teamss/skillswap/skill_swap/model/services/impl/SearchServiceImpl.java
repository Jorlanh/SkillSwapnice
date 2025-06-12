package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommunityRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final PostRepository postRepository;

    @Autowired
    public SearchServiceImpl(UserRepository userRepository, CommunityRepository communityRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.postRepository = postRepository;
    }

    @Override
    public List<Object> search(String query, String filter, String sortBy) {
        List<Object> results = new ArrayList<>();

        if (filter.equalsIgnoreCase("ALL") || filter.equalsIgnoreCase("PROFILE")) {
            List<User> users = userRepository.findAll().stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                            u.getEmail().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            results.addAll(users);
        }

        if (filter.equalsIgnoreCase("ALL") || filter.equalsIgnoreCase("COMMUNITY")) {
            List<Community> communities = communityRepository.findAll().stream()
                    .filter(c -> c.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            results.addAll(communities);
        }

        if (filter.equalsIgnoreCase("ALL") || filter.equalsIgnoreCase("CONTENT")) {
            List<Post> posts = postRepository.findAll().stream()
                    .filter(p -> p.getContent().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            results.addAll(posts);
        }

        if (sortBy.equalsIgnoreCase("RELEVANCE")) {
            results.sort((a, b) -> {
                int scoreA = a instanceof Post ? ((Post) a).getLikesCount() : 0;
                int scoreB = b instanceof Post ? ((Post) b).getLikesCount() : 0;
                return Integer.compare(scoreB, scoreA);
            });
        } else if (sortBy.equalsIgnoreCase("DATE")) {
            results.sort((a, b) -> {
                // ALTERADO: removido .toInstant()
                Instant timeA = a instanceof Post ? ((Post) a).getCreatedAt() : Instant.now();
                Instant timeB = b instanceof Post ? ((Post) b).getCreatedAt() : Instant.now();
                return timeB.compareTo(timeA);
            });
        }

        return results;
    }
}