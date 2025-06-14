package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ErrorResponse;
import br.com.teamss.skillswap.skill_swap.dto.RegisterDTO;
import br.com.teamss.skillswap.skill_swap.dto.SuccessResponse;
import br.com.teamss.skillswap.skill_swap.dto.VerifyDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository; 

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("As senhas não coincidem."));
        }
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("E-mail já registrado."));
        }
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Nome de usuário já registrado."));
        }

        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setName(registerDTO.getName());
        user.setEmail(registerDTO.getEmail());
        user.setUsername(registerDTO.getUsername());
        
        LocalDate localBirthDate = registerDTO.getBirthDate();
        if (localBirthDate != null) {
            user.setBirthDate(Date.valueOf(localBirthDate));
        }
        user.setPhoneNumber(registerDTO.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setCreatedAt(Instant.now());
        user.setVerified(false);

         // ---> INÍCIO DA ALTERAÇÃO <---
        // Cria um novo perfil VAZIO
        Profile newProfile = new Profile();
        // Associa o perfil ao usuário
        user.setProfile(newProfile);
        // Associa o usuário ao perfil (relação bidirecional)
        newProfile.setUser(user);
        // ---> FIM DA ALTERAÇÃO <---
        
        String verificationCode = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(Instant.now().plus(15, ChronoUnit.MINUTES));

        userRepository.save(user);
        emailService.sendVerificationCode(registerDTO.getEmail(), verificationCode);

        return ResponseEntity.ok(new SuccessResponse("Cadastro realizado com sucesso! Verifique seu e-mail."));
    }

    // // Endpoint para reenviar o código de verificação
    // @PostMapping("/register/resend-code")
    // public ResponseEntity<?> resendCode(@RequestBody ResendCodeDTO resendCodeDTO) {
    //     Optional<User> userOpt = userRepository.findByEmail(resendCodeDTO.getEmail());
    //     if (!userOpt.isPresent()) {
    //         return ResponseEntity.badRequest().body(new ErrorResponse("Usuário não encontrado."));
    //     }
    //     User user = userOpt.get();

    //     if (user.getVerified()) {
    //         return ResponseEntity.badRequest().body(new ErrorResponse("Conta já verificada."));
    //     }

    //     // Verificar se o código expirou
    //     if (user.getVerificationCodeExpiry() != null && user.getVerificationCodeExpiry().before(java.sql.Timestamp.valueOf(LocalDateTime.now()))) {
    //         return ResponseEntity.badRequest().body(new ErrorResponse("Código de verificação expirou. Solicite um novo."));
    //     }

    //     String deliveryMethod = resendCodeDTO.getDeliveryMethod();
    //     if ("email".equals(deliveryMethod)) {
    //         emailService.sendVerificationCode(resendCodeDTO.getEmail(), user.getVerificationCode());
    //     } else if ("phone".equals(deliveryMethod)) {
    //         emailService.sendVerificationCodeViaSMS(resendCodeDTO.getPhoneNumber(), user.getVerificationCode());
    //     } else {
    //         return ResponseEntity.badRequest().body(new ErrorResponse("Método de entrega inválido."));
    //     }

    //     user.setUpdatedAt(java.sql.Timestamp.valueOf(LocalDateTime.now()));
    //     userRepository.save(user);

    //     return ResponseEntity.ok(new SuccessResponse("Código reenviado com sucesso!"));
    // }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyDTO verifyDTO) {
        Optional<User> userOpt = userRepository.findByVerificationCode(verifyDTO.getVerificationCode());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Código de verificação inválido."));
        }
        User user = userOpt.get();

        if (user.getVerified()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Conta já verificada."));
        }

        if (user.getVerificationCodeExpiry() != null && Instant.now().isAfter(user.getVerificationCodeExpiry())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Código de verificação expirou."));
        }

        user.setVerified(true);
        user.setVerifiedAt(Instant.now());
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(new SuccessResponse("Conta verificada com sucesso!"));
    }
}