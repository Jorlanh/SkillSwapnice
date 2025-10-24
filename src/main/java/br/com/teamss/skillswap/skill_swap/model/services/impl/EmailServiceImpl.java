package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage; // Importação do SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender; // Importação do JavaMailSender
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailServiceImpl implements EmailService {

    // Utiliza o JavaMailSender injetado pelo Spring
    @Autowired
    private JavaMailSender mailSender;

    // Obtém o email remetente configurado no application.properties (spring.mail.username)
    @Value("${spring.mail.username}") 
    private String mailFrom;

    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;
    
    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    private final ConcurrentHashMap<String, WebSocketSession> sessions;
    
    public EmailServiceImpl(ConcurrentHashMap<String, WebSocketSession> sessions) {
        this.sessions = sessions;
    }

    // Método de envio de notificação usando Spring Mail
    @Override
    public void sendNotification(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception ex) {
            // Lança RuntimeException para que o Controller capture e retorne um erro 500/400
            // Se a senha do App Password estiver errada, o erro virá daqui.
            throw new RuntimeException("Falha ao enviar email via Spring Mail: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        String subject = "SkillSwap - Código de Verificação";
        String body = "Seu código de verificação para o SkillSwap é: " + code + "\nDigite este código na tela de verificação para ativar sua conta.";
        sendNotification(to, subject, body);
    }

    @Override
    public void sendVerificationCodeViaSMS(String phoneNumber, String code) {
        Twilio.init(twilioAccountSid, twilioAuthToken);
        String messageBody = "Seu código de verificação para o SkillSwap é: " + code;
        Message.creator(
            new com.twilio.type.PhoneNumber(phoneNumber),
            new com.twilio.type.PhoneNumber(twilioPhoneNumber),
            messageBody
        ).create();
    }

    @Override
    public void sendLessonNotification(String toEmail, String lessonDetails) {
        sendNotification(toEmail, "Lembrete de Aula", "Lembrete de Aula: " + lessonDetails);
    }

    @Override
    public void sendPlatformNotification(String message) {
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.out.println("Erro ao enviar notificação na plataforma: " + e.getMessage());
                }
            }
        }
    }
}
