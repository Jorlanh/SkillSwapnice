package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}") 
    private String sendgridFromEmail;

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

    @Override
    public void sendNotification(String to, String subject, String body) {
        // CORREÇÃO: Usando a propriedade injetada
        Email from = new Email(sendgridFromEmail); 
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, toEmail, content);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                throw new RuntimeException("Failed to send email: " + response.getBody());
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error sending email: " + ex.getMessage());
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