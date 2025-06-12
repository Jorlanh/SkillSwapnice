package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.AchievementService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AchievementServiceImpl implements AchievementService {

    @Override
    public List<String> getAchievements(UUID userId) {
        // Simulação de conquistas (implementar lógica real)
        return List.of("Primeiro Post", "10 Seguidores");
    }
}