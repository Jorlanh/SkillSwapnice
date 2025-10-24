package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;

import java.util.List;
import java.util.UUID;

public interface UserServiceDTO {
    public UserDTO toUserDTO(User user);
    public List<UserSummaryDTO> findAllSummaries();
    public UserDTO findByIdDTO(UUID id); // Para dados completos (privado)
    public UserSummaryDTO findSummaryByIdDTO(UUID id); // Para dados públicos
    void updateVerificationCode(UUID userId, String code);
    void updateVerificationStatus(UUID userId, boolean verified);
    void saveUserDTO(UserDTO userDTO);
    public UserDTO findByUsernameDTO(String username);
}