package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
}