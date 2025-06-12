package br.com.teamss.skillswap.skill_swap.model.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.teamss.skillswap.skill_swap.model.entities.Skill;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {}
