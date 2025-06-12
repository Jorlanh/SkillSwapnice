package br.com.teamss.skillswap.skill_swap.model.services;

import java.util.List;
import java.util.UUID;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;

public interface UserServiceDTO {
    public UserDTO toUserDTO(User user);
    public List<UserDTO> findAllDTO();
    public UserDTO findByIdDTO(UUID id);
    void updateVerificationCode(UUID userId, String code);
    void updateVerificationStatus(UUID userId, boolean verified);
    void saveUserDTO(UserDTO userDTO);
    public UserDTO findByUsernameDTO(String username);
}