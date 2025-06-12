package br.com.teamss.skillswap.skill_swap.model.services;

import java.util.List;
import java.util.UUID;

public interface AchievementService {
    List<String> getAchievements(UUID userId);
}