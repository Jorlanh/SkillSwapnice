package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LocalUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocalUserServiceImpl implements LocalUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User findOrCreateUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new IllegalArgumentException("Token JWT não contém a claim 'email'.");
        }

        // Procura o usuário. Se não existir, cria um novo.
        return userRepository.findByEmail(email).orElseGet(() -> createUserFromJwt(jwt, email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private User createUserFromJwt(Jwt jwt, String email) {
        User newUser = new User();
        // A melhor prática é usar a claim 'sub' (subject) do Auth0 como o ID.
        // Ela é garantidamente única. Ex: "google-oauth2|105299..."
        // Meu banco espera um UUID, isso vai gerar um.
        newUser.setUserId(UUID.randomUUID()); 
        newUser.setEmail(email);

        String username = jwt.getClaimAsString("nickname");
        if (username == null || userRepository.existsByUsername(username)) {
            // Se o username não existir ou já estiver em uso, cria um username único
            username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);
        }
        newUser.setUsername(username);
        
        newUser.setName(jwt.getClaimAsString("name"));
        
        // Define uma senha aleatória e inutilizável
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        
        newUser.setVerified(jwt.getClaimAsBoolean("email_verified")); // Pega a verificação direto do Auth0
        newUser.setVerifiedAt(Instant.now());
        newUser.setCreatedAt(Instant.now());
        
        return userRepository.save(newUser);
    }
}