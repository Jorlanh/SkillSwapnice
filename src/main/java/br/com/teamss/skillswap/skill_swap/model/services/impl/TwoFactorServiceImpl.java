package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.TwoFactorConfig;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.TwoFactorConfigRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.TwoFactorService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant; // ALTERADO
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final Logger logger = LoggerFactory.getLogger(TwoFactorServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwoFactorConfigRepository twoFactorConfigRepository;

    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @Value("${sendgrid.api.key}")
    private String sendgridApiKey;

    @Value("${sendgrid.from.email}")
    private String sendgridFromEmail;

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    @PostConstruct
    public void initTwilio() {
        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            logger.info("Twilio inicializado com Account SID: {}", twilioAccountSid);
        } catch (Exception e) {
            logger.error("Erro ao inicializar Twilio: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao inicializar Twilio", e);
        }
    }

    @Override
    public boolean isAnyMethodEnabled(UUID userId) {
        List<TwoFactorConfig> configs = twoFactorConfigRepository.findByUserId(userId);
        logger.debug("Verificando métodos 2FA para usuário {}. Encontradas {} configurações.", userId, configs.size());
        return configs.stream().anyMatch(TwoFactorConfig::isEnabled);
    }

    @Override
    public String generateCode(UUID userId, String method) {
        Optional<TwoFactorConfig> configOpt = twoFactorConfigRepository.findByUserIdAndMethod(userId, method);
        if (!configOpt.isPresent() || !configOpt.get().isEnabled()) {
            logger.error("Método 2FA {} não ativado para usuário {}", method, userId);
            throw new RuntimeException("Método de 2FA " + method + " não está ativado para o usuário");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            logger.error("Usuário não encontrado: {}", userId);
            throw new RuntimeException("Usuário não encontrado");
        }
        User user = userOpt.get();

        if ("GOOGLE_AUTH".equals(method)) {
            TwoFactorConfig config = configOpt.get();
            String secret = config.getSecret();
            if (secret == null) {
                secret = generateSecret();
                config.setSecret(secret);
                twoFactorConfigRepository.save(config);
                logger.info("Gerado novo segredo Google Auth para usuário {}", userId);
            }
            return secret;
        } else if ("EMAIL".equals(method)) {
            String code = generateVerificationCode();
            String email = user.getEmail();
            if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                logger.error("E-mail inválido ou ausente para usuário {}: {}", userId, email);
                throw new RuntimeException("E-mail inválido ou não configurado para o usuário");
            }
            sendEmail(email, code, userId);
            user.setVerificationCode(code);
            user.setVerificationCodeExpiry(Instant.now().plus(10, ChronoUnit.MINUTES)); // ALTERADO
            userRepository.save(user);
            logger.info("Gerado código de verificação por e-mail para usuário {}", userId);
            return code;
        } else if ("SMS".equals(method)) {
            String code = generateVerificationCode();
            String phoneNumber = normalizePhoneNumber(user.getPhoneNumber());
            if (phoneNumber == null) {
                logger.error("Número de telefone inválido ou ausente para usuário {}: {}", userId, user.getPhoneNumber());
                throw new RuntimeException("Número de telefone inválido ou não configurado para o usuário");
            }
            sendSMS(phoneNumber, code, userId);
            user.setVerificationCode(code);
            user.setVerificationCodeExpiry(Instant.now().plus(10, ChronoUnit.MINUTES)); // ALTERADO
            userRepository.save(user);
            logger.info("Gerado código de verificação por SMS para usuário {}", userId);
            return code;
        }
        logger.error("Método 2FA não suportado: {} para usuário {}", method, userId);
        throw new RuntimeException("Método de 2FA não suportado: " + method);
    }

    @Override
    public boolean verifyCode(UUID userId, String method, String code) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            logger.warn("Usuário não encontrado para verificação: {}", userId);
            return false;
        }
        User user = userOpt.get();

        Optional<TwoFactorConfig> configOpt = twoFactorConfigRepository.findByUserIdAndMethod(userId, method);
        if (!configOpt.isPresent() || !configOpt.get().isEnabled()) {
            logger.warn("Método 2FA {} não ativado para usuário {}", method, userId);
            return false;
        }

        if ("GOOGLE_AUTH".equals(method)) {
            String secret = configOpt.get().getSecret();
            if (secret == null) {
                logger.warn("Segredo Google Auth não encontrado para usuário {}", userId);
                return false;
            }
            try {
                boolean isValid = googleAuthenticator.authorize(secret, Integer.parseInt(code));
                logger.info("Verificação de código Google Auth para usuário {}: {}", userId, isValid ? "sucesso" : "falha");
                return isValid;
            } catch (NumberFormatException e) {
                logger.error("Formato de código Google Auth inválido para usuário {}: {}", userId, code);
                return false;
            }
        } else if ("EMAIL".equals(method) || "SMS".equals(method)) {
            if (user.getVerificationCode() == null || user.getVerificationCodeExpiry() == null) {
                logger.warn("Código de verificação ou expiração não definidos para usuário {} e método {}", userId, method);
                return false;
            }
            Instant expiryInstant = user.getVerificationCodeExpiry(); // ALTERADO
            if (Instant.now().isAfter(expiryInstant)) {
                logger.warn("Código expirado para usuário {} e método {}", userId, method);
                return false;
            }
            boolean isValid = user.getVerificationCode().equals(code);
            logger.info("Verificação de código {} para usuário {}: {}", method, userId, isValid ? "sucesso" : "falha");
            return isValid;
        }
        logger.warn("Método 2FA inválido {} para usuário {}", method, userId);
        return false;
    }

    @Override
    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        logger.debug("Gerado novo segredo Google Auth");
        return key.getKey();
    }

    @Override
    public String generateQRCodeImage(String user, String secret) throws WriterException {
        String qrCodeData = String.format("otpauth://totp/SkillSwap:%s?secret=%s&issuer=SkillSwap", user, secret);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        try {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            String qrCodeImage = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
            logger.debug("Gerado imagem de QR code para usuário {}", user);
            return qrCodeImage;
        } catch (IOException e) {
            logger.error("Falha ao gerar imagem de QR code para usuário {}: {}", user, e.getMessage(), e);
            throw new RuntimeException("Falha ao gerar imagem de QR code", e);
        }
    }

    @Override
    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Gera código de 6 dígitos
        logger.debug("Gerado código de verificação: {}", code);
        return String.valueOf(code);
    }

    @Override
    public void sendSMS(String to, String code, UUID userId) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioPhoneNumber),
                    "Seu código de verificação SkillSwap é: " + code
            ).create();
            logger.info("SMS enviado para {} para usuário {}. SID: {}", to, userId, message.getSid());
        } catch (Exception e) {
            logger.error("Falha ao enviar SMS para {} para usuário {}: {}", to, userId, e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar SMS", e);
        }
    }

    @Override
    public void sendEmail(String to, String code, UUID userId) {
        SendGrid sg = new SendGrid(sendgridApiKey);
        Email from = new Email(sendgridFromEmail);
        Email toEmail = new Email(to);
        String subject = "Código de Verificação SkillSwap";
        Content content = new Content("text/html",
                "<p>Olá,</p>" +
                        "<p>Seu código de verificação para o SkillSwap é: <strong>" + code + "</strong></p>" +
                        "<p>Este código é válido por 10 minutos.</p>" +
                        "<p>Se você não solicitou este código, ignore este e-mail.</p>" +
                        "<p>Atenciosamente,<br>Equipe SkillSwap</p>"
        );
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            // Envia a requisição para o SendGrid
            Response response = sg.api(request);
            logger.info("E-mail enviado para {} para usuário {}. Status: {}", to, userId, response.getStatusCode());
            if (response.getStatusCode() >= 400) {
                logger.error("Falha ao enviar e-mail para {} para usuário {}. Status: {}, Resposta: {}",
                        to, userId, response.getStatusCode(), response.getBody());
                throw new RuntimeException("Falha ao enviar e-mail: Status " + response.getStatusCode());
            }
        } catch (IOException e) {
            logger.error("Erro ao enviar e-mail para {} para usuário {}: {}", to, userId, e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar e-mail", e);
        }
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("[^0-9+]", "");
        if (!phone.startsWith("+")) {
            phone = "+1" + phone;
        }
        if (!phone.matches("\\+\\d{10,15}")) {
            logger.warn("Formato de número de telefone inválido: {}", phone);
            return null;
        }
        logger.debug("Número de telefone normalizado: {}", phone);
        return phone;
    }
}