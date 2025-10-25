package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPrivateProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPublicProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserServiceDTO userServiceDTO;

    @Autowired
    private UserRepository userRepository; // Adicionado para buscar o usuário completo

    public UserController(UserService userService, UserServiceDTO userServiceDTO) {
        this.userServiceDTO = userServiceDTO;
        this.userService = userService;
    }

    @GetMapping
    public List<UserSummaryDTO> getAllUsers() {
        return userServiceDTO.findAllSummaries();
    }

    // ENDPOINT PÚBLICO: Retorna dados públicos de qualquer usuário pelo username. NÃO expõe o UUID.
    @GetMapping("/{username}")
    public ResponseEntity<UserPublicProfileDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userServiceDTO.findPublicProfileByUsername(username));
    }

    // ENDPOINT PRIVADO: Retorna os dados completos, incluindo UUID, APENAS do usuário autenticado.
    @GetMapping("/me")
    public ResponseEntity<UserPrivateProfileDTO> getMyProfile() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        // Recarrega a entidade User para garantir que todos os dados (incluindo roles) estejam presentes
        User user = userRepository.findById(authenticatedUser.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado no banco de dados."));
        return ResponseEntity.ok(userServiceDTO.toUserPrivateProfileDTO(user));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody User user) {
        User savedUser = userService.save(user);
        UserDTO userDTO = userServiceDTO.toUserDTO(savedUser);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/{userId}/skills")
    public ResponseEntity<UserDTO> addSkillsToUser(
            @PathVariable UUID userId,
            @RequestBody List<Long> skillIds) {

        User updatedUser = userService.addSkills(userId, skillIds);
        UserDTO userDTO = userServiceDTO.toUserDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<UserDTO> addRolesToUser(
            @PathVariable UUID userId,
            @RequestBody List<Long> roleIds) {

        User updatedUser = userService.addRoles(userId, roleIds);
        UserDTO userDTO = userServiceDTO.toUserDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody User user) {
        user.setUserId(id);
        User updatedUser = userService.update(id, user);
        UserDTO userDTO = userServiceDTO.toUserDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}