package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ErrorResponse;
import br.com.teamss.skillswap.skill_swap.dto.LoginRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LoginResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LoginService;
import br.com.teamss.skillswap.skill_swap.model.services.SecurityAuditService;
import br.com.teamss.skillswap.skill_swap.model.services.impl.LoginServiceImpl.TwoFactorAuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAuditService auditService;

    @PostMapping
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        try {
            String jwtToken = loginService.authenticateAndGetToken(loginRequest);
            auditService.logLoginSuccess(loginRequest.getUsernameOrEmail(), request);
            return ResponseEntity.ok(new LoginResponseDTO(jwtToken, "Login bem-sucedido!"));
        } catch (BadCredentialsException e) {
            auditService.logLoginFailure(loginRequest.getUsernameOrEmail(), "Credenciais inválidas", request);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        } catch (TwoFactorAuthenticationRequiredException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            auditService.logLoginFailure(loginRequest.getUsernameOrEmail(), e.getMessage(), request);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verifyTwoFactor(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsernameOrEmail())
                    .or(() -> userRepository.findByEmail(loginRequest.getUsernameOrEmail()));

            if (userOpt.isEmpty()) {
                auditService.logLoginFailure(loginRequest.getUsernameOrEmail(), "Usuário não encontrado durante 2FA", request);
                return ResponseEntity.badRequest().body(new ErrorResponse("Usuário não encontrado."));
            }

            User user = userOpt.get();
            String twoFactorCode = loginRequest.getTwoFactorCode();
            String twoFactorMethod = loginRequest.getTwoFactorMethod();

            if (twoFactorCode == null || twoFactorMethod == null) {
                auditService.logLoginFailure(user.getUsername(), "Código ou método 2FA não fornecido", request);
                return ResponseEntity.badRequest().body(new ErrorResponse("Código ou método 2FA não fornecido."));
            }

            if (loginService.verifyTwoFactorCode(user.getUserId(), twoFactorMethod, twoFactorCode)) {
                String jwtToken = loginService.authenticateAndGetToken(loginRequest);
                auditService.logLoginSuccess(user.getUsername(), request);
                return ResponseEntity.ok(new LoginResponseDTO(jwtToken, "Login 2FA bem-sucedido!"));
            } else {
                auditService.logLoginFailure(user.getUsername(), "Código 2FA inválido ou expirado", request);
                return ResponseEntity.badRequest().body(new ErrorResponse("Código 2FA inválido ou expirado."));
            }
        } catch (BadCredentialsException e) {
            auditService.logLoginFailure(loginRequest.getUsernameOrEmail(), "Credenciais inválidas durante 2FA", request);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            auditService.logLoginFailure(loginRequest.getUsernameOrEmail(), e.getMessage(), request);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/google")
    public void loginWithGoogle(HttpServletRequest request, HttpServletResponse response) {
        loginService.loginWithGoogle(request, response);
    }
}