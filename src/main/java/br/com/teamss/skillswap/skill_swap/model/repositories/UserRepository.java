package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username); // Verifica se o username já existe
    Optional<User> findByVerificationCode(String verificationCode); // Ajustado para Optional
    Optional<User> findByEmail(String email); // Ajustado para Optional
    Optional<User> findByUsername(String username); // Adicionado conforme solicitado
}