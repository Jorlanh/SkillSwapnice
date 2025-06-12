package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.LoginRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LoginResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.ErrorResponse;
import br.com.teamss.skillswap.skill_swap.dto.SuccessResponse;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            String jwtToken = loginService.authenticateAndGetToken(loginRequest);
            return ResponseEntity.ok(new LoginResponseDTO(jwtToken, "Login bem-sucedido!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verifyTwoFactor(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest request, HttpServletResponse response) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsernameOrEmail())
                    .or(() -> userRepository.findByEmail(loginRequest.getUsernameOrEmail()));
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Usuário não encontrado."));
            }
            User user = userOpt.get();
            String twoFactorCode = loginRequest.getTwoFactorCode();
            String twoFactorMethod = loginRequest.getTwoFactorMethod();
            if (twoFactorCode != null && twoFactorMethod != null) {
                if (loginService.verifyTwoFactorCode(user.getUserId(), twoFactorMethod, twoFactorCode)) {
                    loginService.redirectToHomepage(response);
                    return ResponseEntity.ok(new SuccessResponse("Login concluído com sucesso!"));
                } else {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Código 2FA inválido."));
                }
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("Código ou método 2FA não fornecido."));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/google")
    public ResponseEntity<?> loginWithGoogle(HttpServletRequest request, HttpServletResponse response) {
        try {
            loginService.loginWithGoogle(request, response);
            return ResponseEntity.ok(new SuccessResponse("Login com Google iniciado com sucesso!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/register")
    public ResponseEntity<?> redirectToRegister(HttpServletResponse response) {
        try {
            loginService.redirectToRegister(response);
            return ResponseEntity.ok(new SuccessResponse("Redirecionado para registro."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}