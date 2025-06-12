package br.com.teamss.skillswap.skill_swap.model.services;

import com.google.zxing.WriterException;

import java.util.UUID;

public interface TwoFactorService {
    boolean isAnyMethodEnabled(UUID userId);
    String generateCode(UUID userId, String method);
    boolean verifyCode(UUID userId, String method, String code);

    // Métodos usados pelo TwoFactorController
    String generateSecret();
    String generateQRCodeImage(String user, String secret) throws WriterException;
    String generateVerificationCode();
    void sendSMS(String to, String code, UUID userId);
    void sendEmail(String to, String code, UUID userId);
}