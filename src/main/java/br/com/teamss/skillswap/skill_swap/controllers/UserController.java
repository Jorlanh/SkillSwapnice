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
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserServiceDTO userServiceDTO;

    @Autowired
    private UserRepository userRepository;

    public UserController(UserService userService, UserServiceDTO userServiceDTO) {
        this.userServiceDTO = userServiceDTO;
        this.userService = userService;
    }

    @GetMapping
    public List<UserSummaryDTO> getAllUsers() {
        return userServiceDTO.findAllSummaries();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserPublicProfileDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userServiceDTO.findPublicProfileByUsername(username));
    }

    @GetMapping("/me")
    public ResponseEntity<UserPrivateProfileDTO> getMyProfile() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        User user = userRepository.findById(authenticatedUser.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado no banco de dados."));
        return ResponseEntity.ok(userServiceDTO.toUserPrivateProfileDTO(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(@RequestBody User user) {
        User savedUser = userService.save(user);
        UserDTO userDTO = userServiceDTO.toUserDTO(savedUser);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/{userId}/skills")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> addSkillsToUser(
            @PathVariable UUID userId,
            @RequestBody List<Long> skillIds) {
        User updatedUser = userService.addSkills(userId, skillIds);
        UserDTO userDTO = userServiceDTO.toUserDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> addRolesToUser(
            @PathVariable UUID userId,
            @RequestBody List<Long> roleIds) {
        User updatedUser = userService.addRoles(userId, roleIds);
        UserDTO userDTO = userServiceDTO.toUserDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.username")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody User user) {
        user.setUserId(id);
        User updatedUser = userService.update(id, user);
        UserDTO userDTO = userServiceDTO.toUserDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}