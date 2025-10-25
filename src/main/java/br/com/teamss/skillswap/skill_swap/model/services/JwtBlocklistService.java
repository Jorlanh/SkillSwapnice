package br.com.teamss.skillswap.skill_swap.model.services;

import java.time.Duration;

public interface JwtBlocklistService {
    void blockToken(String token, Duration ttl);
    boolean isTokenBlocked(String token);
}