package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.LoginRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LoginResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.ErrorResponse;
import br.com.teamss.skillswap.skill_swap.dto.SuccessResponse;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LoginService;
import br.com.teamss.skillswap.skill_swap.model.services.impl.LoginServiceImpl.TwoFactorAuthenticationRequiredException; // Import da exceção customizada
import jakarta.validation.Valid; // Import @Valid
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException; // Import BadCredentialsException
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) { // Adicionado @Valid
        try {
            String jwtToken = loginService.authenticateAndGetToken(loginRequest);
            return ResponseEntity.ok(new LoginResponseDTO(jwtToken, "Login bem-sucedido!"));
        } catch (BadCredentialsException e) {
            // Retorna 401 Unauthorized para credenciais inválidas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        } catch (TwoFactorAuthenticationRequiredException e) {
            // Retorna um status específico (ex: 403 Forbidden ou um customizado)
            // e uma mensagem indicando a necessidade de 2FA
            // O frontend pode usar essa resposta para exibir o formulário 2FA
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) { // Captura outras exceções genéricas
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-2fa")
    // Adicionado @Valid
    public ResponseEntity<?> verifyTwoFactor(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsernameOrEmail())
                    .or(() -> userRepository.findByEmail(loginRequest.getUsernameOrEmail()));

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Usuário não encontrado."));
            }

            User user = userOpt.get();
            String twoFactorCode = loginRequest.getTwoFactorCode();
            String twoFactorMethod = loginRequest.getTwoFactorMethod(); // Certifique-se que o frontend envia isso

            if (twoFactorCode == null || twoFactorMethod == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Código ou método 2FA não fornecido."));
            }

            if (loginService.verifyTwoFactorCode(user.getUserId(), twoFactorMethod, twoFactorCode)) {
                // Se 2FA válido, gerar o token JWT final e retornar
                String jwtToken = loginService.authenticateAndGetToken(loginRequest); // Reautentica para gerar token
                return ResponseEntity.ok(new LoginResponseDTO(jwtToken, "Login 2FA bem-sucedido!"));
                // loginService.redirectToHomepage(response); // Remover redirecionamento em API REST
                // return ResponseEntity.ok(new SuccessResponse("Login concluído com sucesso!"));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("Código 2FA inválido ou expirado."));
            }
        } catch (BadCredentialsException e) {
             // Caso a senha esteja errada mesmo na verificação 2FA
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // A lógica de login com Google/OAuth2 geralmente é tratada por filtros do Spring Security
    // Este endpoint pode ser usado como callback ou ponto de entrada inicial
    @GetMapping("/google")
    public void loginWithGoogle(HttpServletRequest request, HttpServletResponse response) {
         // O redirecionamento para o provedor OAuth2 geralmente é iniciado pelo frontend
         // ou por uma configuração do Spring Security OAuth2 Client.
         // Este endpoint pode ser o callback configurado no provedor.
        loginService.loginWithGoogle(request, response);
        // A resposta após o callback OAuth2 geralmente envolve gerar um token JWT
        // e retorná-lo, ou redirecionar o usuário para o frontend com o token.
    }

    // Este endpoint não faz muito sentido em uma API REST
    // @GetMapping("/register")
    // public ResponseEntity<?> redirectToRegister(HttpServletResponse response) {
    //     try {
    //         loginService.redirectToRegister(response);
    //         return ResponseEntity.ok(new SuccessResponse("Redirecionado para registro."));
    //     } catch (RuntimeException e) {
    //         return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    //     }
    // }
}