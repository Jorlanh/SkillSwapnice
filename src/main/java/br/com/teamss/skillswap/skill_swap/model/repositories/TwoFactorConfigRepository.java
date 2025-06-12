package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.TwoFactorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TwoFactorConfigRepository extends JpaRepository<TwoFactorConfig, Long> {
    List<TwoFactorConfig> findByUserId(UUID userId);
    Optional<TwoFactorConfig> findByUserIdAndMethod(UUID userId, String method);
}