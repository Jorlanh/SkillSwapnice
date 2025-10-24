package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ProposalRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ProposalResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.ProposalService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProposalServiceImpl implements ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public Proposal sendProposal(ProposalRequestDTO proposalRequest) {
        User sender = userRepository.findById(proposalRequest.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário remetente não encontrado"));
        
        User receiver = userRepository.findById(proposalRequest.getReceiverId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário destinatário não encontrado"));

        Skill offeredSkill = skillRepository.findById(proposalRequest.getOfferedSkillId())
                .orElseThrow(() -> new EntityNotFoundException("Habilidade oferecida não encontrada"));

        Skill requestedSkill = skillRepository.findById(proposalRequest.getRequestedSkillId())
                .orElseThrow(() -> new EntityNotFoundException("Habilidade solicitada não encontrada"));

        Proposal proposal = new Proposal();
        proposal.setSender(sender);
        proposal.setReceiver(receiver);
        proposal.setOfferedSkill(offeredSkill);
        proposal.setRequestedSkill(requestedSkill);
        proposal.setStatus("PENDING");
        proposal.setCreatedAt(Instant.now());
        proposal.setUpdatedAt(Instant.now());
        
        Proposal savedProposal = proposalRepository.save(proposal);

        String receiverEmail = savedProposal.getReceiver().getEmail();
        String messageContent = "Você recebeu uma proposta de troca de habilidades de " + savedProposal.getSender().getUsername() +
                               ": Oferece " + savedProposal.getOfferedSkill().getName() +
                               " em troca de " + savedProposal.getRequestedSkill().getName();
        
        emailService.sendNotification(receiverEmail, "Nova Proposta no SkillSwap", messageContent);
        
        return savedProposal;
    }

    @Override
    public List<ProposalResponseDTO> getUserProposals(UUID userId) {
        List<Proposal> proposals = proposalRepository.findBySenderIdOrReceiverId(userId);
        
        return proposals.stream().map(proposal -> {
            ProposalResponseDTO dto = new ProposalResponseDTO();
            dto.setProposalId(proposal.getProposalId());
            dto.setSender(new UserSummaryDTO(proposal.getSender().getUserId(), proposal.getSender().getUsername(), proposal.getSender().getName()));
            dto.setReceiver(new UserSummaryDTO(proposal.getReceiver().getUserId(), proposal.getReceiver().getUsername(), proposal.getReceiver().getName()));
            dto.setOfferedSkill(proposal.getOfferedSkill());
            dto.setRequestedSkill(proposal.getRequestedSkill());
            dto.setStatus(proposal.getStatus());
            dto.setCreatedAt(proposal.getCreatedAt());
            dto.setUpdatedAt(proposal.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Proposal acceptProposal(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        if (!proposal.getStatus().equals("PENDING")) {
            throw new IllegalStateException("A proposta não está mais pendente");
        }

        proposal.setStatus("ACCEPTED");
        proposal.setUpdatedAt(Instant.now());
        Proposal updatedProposal = proposalRepository.save(proposal);

        String senderEmail = proposal.getSender().getEmail();
        String messageContent = "Sua proposta foi aceita por " + proposal.getReceiver().getUsername() +
                               ": Você ofereceu " + proposal.getOfferedSkill().getName() +
                               " em troca de " + proposal.getRequestedSkill().getName();
        emailService.sendNotification(senderEmail, "Proposta Aceita no SkillSwap", messageContent);
        emailService.sendPlatformNotification("Proposta aceita por " + proposal.getReceiver().getUsername() + ": " + messageContent);

        Notification notification = new Notification();
        notification.setUser(proposal.getSender());
        notification.setMessage(messageContent);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        return updatedProposal;
    }

    @Override
    public Proposal rejectProposal(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        if (!proposal.getStatus().equals("PENDING")) {
            throw new IllegalStateException("A proposta não está mais pendente");
        }

        proposal.setStatus("REJECTED");
        proposal.setUpdatedAt(Instant.now());
        Proposal updatedProposal = proposalRepository.save(proposal);

        String senderEmail = proposal.getSender().getEmail();
        String messageContent = "Sua proposta foi rejeitada por " + proposal.getReceiver().getUsername();
        emailService.sendNotification(senderEmail, "Proposta Rejeitada no SkillSwap", messageContent);
        emailService.sendPlatformNotification("Proposta rejeitada por " + proposal.getReceiver().getUsername());

        Notification notification = new Notification();
        notification.setUser(proposal.getSender());
        notification.setMessage(messageContent);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        return updatedProposal;
    }

    @Override
    public Proposal blockProposal(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        if (!proposal.getStatus().equals("PENDING")) {
            throw new IllegalStateException("A proposta não está mais pendente");
        }

        proposal.setStatus("BLOCKED");
        proposal.setUpdatedAt(Instant.now());
        Proposal updatedProposal = proposalRepository.save(proposal);

        String senderEmail = proposal.getSender().getEmail();
        String messageContent = "Sua proposta foi bloqueada por " + proposal.getReceiver().getUsername();
        emailService.sendNotification(senderEmail, "Proposta Bloqueada no SkillSwap", messageContent);
        emailService.sendPlatformNotification("Proposta bloqueada por " + proposal.getReceiver().getUsername());

        Notification notification = new Notification();
        notification.setUser(proposal.getSender());
        notification.setMessage(messageContent);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        return updatedProposal;
    }
}