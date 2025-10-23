package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ProposalRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import java.util.List;
import java.util.UUID; // Importar UUID

public interface ProposalService {
    Proposal sendProposal(ProposalRequestDTO proposalRequest);
    // CORRIGIDO: Alterado o tipo do parâmetro de Long para UUID.
    List<Proposal> getUserProposals(UUID userId);
    Proposal acceptProposal(Long proposalId);
    Proposal rejectProposal(Long proposalId);
    Proposal blockProposal(Long proposalId);
}