package br.com.teamss.skillswap.skill_swap.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    // Aqui você injetaria seu serviço de geração de JWT

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {
        // Valida contra os usuários do seu DML (cr7_dev, etc)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.get("username"),
                        loginRequest.get("password")
                )
        );

        // Simulando a geração de um token para o teste de interface
        Map<String, Object> response = new HashMap<>();
        response.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."); // Substitua pela lógica real
        response.put("username", loginRequest.get("username"));
        
        return ResponseEntity.ok(response);
    }
}