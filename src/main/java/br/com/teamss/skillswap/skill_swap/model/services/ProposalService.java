package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ProposalRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import java.util.List;

public interface ProposalService {
    Proposal sendProposal(ProposalRequestDTO proposalRequest);
    List<Proposal> getUserProposals(Long userId);
    Proposal acceptProposal(Long proposalId);
    Proposal rejectProposal(Long proposalId);
    Proposal blockProposal(Long proposalId);
}