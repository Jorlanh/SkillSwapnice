package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID; // Importar UUID

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    // CORRIGIDO: A query agora busca pelo campo userId (que é um UUID) e o parâmetro do método é um UUID.
    @Query("SELECT p FROM Proposal p WHERE p.sender.userId = :userId OR p.receiver.userId = :userId")
    List<Proposal> findBySenderIdOrReceiverId(@Param("userId") UUID userId);
}