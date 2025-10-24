package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;

import java.util.List;
import java.util.UUID;

public interface UserServiceDTO {
    UserDTO toUserDTO(User user);
    List<UserSummaryDTO> findAllSummaries();
    UserDTO findByIdDTO(UUID id);
    UserSummaryDTO findSummaryByIdDTO(UUID id);
    void updateVerificationCode(UUID userId, String code);
    void updateVerificationStatus(UUID userId, boolean verified);
    void saveUserDTO(UserDTO userDTO);
    UserDTO findByUsernameDTO(String username);
    UserDTO getAuthenticatedUser(); // NOVO MÉTODO
}