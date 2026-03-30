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
import org.springframework.web.bind.annotation.*;

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

    // --- LEITURA DE DADOS ---

    @GetMapping
    public List<UserSummaryDTO> getAllUsers() {
        return userServiceDTO.findAllSummaries();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserPublicProfileDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userServiceDTO.findPublicProfileByUsername(username));
    }

    /**
     * ENDPOINT DE IDENTIDADE (SINCRONIZADO COM AUTH0)
     * Se o usuário logou via Auth0 mas ainda não tem perfil no Postgres, 
     * aqui é o momento de validar a existência.
     */
    @GetMapping("/me")
    public ResponseEntity<UserPrivateProfileDTO> getMyProfile() {
        // 1. Obtém os dados básicos vindos do Token JWT (Auth0)
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        
        // 2. Tenta encontrar no banco PostgreSQL
        User user = userRepository.findById(authenticatedUser.getUserId())
            .or(() -> userRepository.findByEmail(authenticatedUser.getEmail())) // Fallback por email se UUID divergir
            .orElseThrow(() -> new EntityNotFoundException("Usuário do Auth0 ainda não sincronizado no banco local."));
            
        return ResponseEntity.ok(userServiceDTO.toUserPrivateProfileDTO(user));
    }

    // --- OPERAÇÕES DE ESCRITA E ADMINISTRAÇÃO ---

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
    @PreAuthorize("hasRole('ADMIN') or @userRepository.findById(#id).get().username == authentication.principal.username")
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