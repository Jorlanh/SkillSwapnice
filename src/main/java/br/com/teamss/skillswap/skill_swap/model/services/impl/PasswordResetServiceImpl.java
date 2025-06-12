package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        String code = String.format("%06d", new Random().nextInt(999999));
        Instant expiry = Instant.now().plus(10, ChronoUnit.MINUTES);

        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(expiry);
        userRepository.save(user);

        emailService.sendVerificationCode(email, code);
    }

    @Override
    public boolean verifyResetCode(String email, String code) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        if (user.getVerificationCode() == null || user.getVerificationCodeExpiry() == null) {
            return false;
        }

        if (!user.getVerificationCode().equals(code)) {
            return false;
        }

        if (Instant.now().isAfter(user.getVerificationCodeExpiry())) {
            user.setVerificationCode(null);
            user.setVerificationCodeExpiry(null);
            userRepository.save(user);
            return false;
        }

        return true;
    }

    @Override
    public void resetPassword(String email, String newPassword, String confirmPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        if (user.getVerificationCode() == null || user.getVerificationCodeExpiry() == null) {
            throw new IllegalStateException("Código de verificação não encontrado ou já utilizado.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);
    }
}