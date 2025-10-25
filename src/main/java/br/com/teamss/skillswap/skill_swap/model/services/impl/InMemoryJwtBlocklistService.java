package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.JwtBlocklistService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class InMemoryJwtBlocklistService implements JwtBlocklistService {

    private final Cache<String, Boolean> blocklist;

    public InMemoryJwtBlocklistService() {
        this.blocklist = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(100_000)
                .build();
    }

    @Override
    public void blockToken(String token, Duration ttl) {
        blocklist.put(token, true);
    }

    @Override
    public boolean isTokenBlocked(String token) {
        return blocklist.getIfPresent(token) != null;
    }
}