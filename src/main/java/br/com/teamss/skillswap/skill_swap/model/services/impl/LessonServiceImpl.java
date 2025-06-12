package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LessonService;
import jakarta.persistence.EntityNotFoundException;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class LessonServiceImpl implements LessonService {

    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository; 

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "SkillSwap";
    private final Calendar calendarService;

    public LessonServiceImpl(LessonRepository lessonRepository, EmailService emailService, NotificationRepository notificationRepository) throws IOException, GeneralSecurityException {
        this.lessonRepository = lessonRepository;
        this.emailService = emailService;
        this.notificationRepository = notificationRepository;

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        InputStream inputStream = this.getClass().getResourceAsStream("/credentials.json");
        if (inputStream == null) {
            throw new IllegalStateException("Arquivo de credenciais 'credentials.json' não encontrado no classpath. Certifique-se de que o arquivo está em src/main/resources.");
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/calendar"));
        this.calendarService = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

   @Override
public Lesson scheduleLesson(LessonScheduleRequestDTO lessonRequest) {
    // Logs existentes
    System.out.println("Iniciando scheduleLesson com DTO: " + lessonRequest);
    System.out.println("teacherId: " + lessonRequest.getTeacherId());
    System.out.println("studentId: " + lessonRequest.getStudentId());
    System.out.println("scheduledTime: " + lessonRequest.getScheduledTime());

    // ADICIONADO: Log para confirmar se os IDs são válidos antes da consulta
    if (lessonRequest.getTeacherId() == null) {
        System.out.println("Erro: teacherId é nulo no DTO");
        throw new IllegalArgumentException("teacherId não pode ser nulo");
    }
    if (lessonRequest.getStudentId() == null) {
        System.out.println("Erro: studentId é nulo no DTO");
        throw new IllegalArgumentException("studentId não pode ser nulo");
    }

    // CORREÇÃO: Busca os usuários completos usando os IDs do DTO
    User teacher = userRepository.findById(lessonRequest.getTeacherId())
            .orElseThrow(() -> new EntityNotFoundException("Professor não encontrado com ID: " + lessonRequest.getTeacherId()));
    User student = userRepository.findById(lessonRequest.getStudentId())
            .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado com ID: " + lessonRequest.getStudentId()));

    Lesson lesson = new Lesson();
    lesson.setTeacher(teacher);
    lesson.setStudent(student);
    lesson.setScheduledTime(lessonRequest.getScheduledTime());
    lesson.setStatus("SCHEDULED");
    createCalendarEvent(lesson, teacher.getEmail(), student.getEmail());
    return lessonRepository.save(lesson);
}

    private void createCalendarEvent(Lesson lesson, String teacherEmail, String studentEmail) {
        Event event = new Event()
                .setSummary("Aula SkillSwap: " + lesson.getTeacher().getUsername() + " e " + lesson.getStudent().getUsername())
                .setDescription("Aula de troca de habilidades.")
                .setStart(new EventDateTime()
                        .setDateTime(new com.google.api.client.util.DateTime(lesson.getScheduledTime().toEpochMilli()))
                        .setTimeZone("America/Sao_Paulo"))
                .setEnd(new EventDateTime()
                        .setDateTime(new com.google.api.client.util.DateTime(
                                lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"))
                                        .plusHours(1).toInstant().toEpochMilli()))
                        .setTimeZone("America/Sao_Paulo"));

        EventAttendee teacher = new EventAttendee().setEmail(teacherEmail);
        EventAttendee student = new EventAttendee().setEmail(studentEmail);
        event.setAttendees(List.of(teacher, student));

        try {
            calendarService.events().insert("primary", event).execute();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar evento no Google Calendar: " + e.getMessage());
        }
    }

    @Override
    public List<Lesson> getUpcomingLessons(UUID userId) {
        // Pega o tempo atual para buscar apenas aulas futuras!
        Instant now = Instant.now();
        
        // Chama o novo método do repositório, passando o userId para ambas as condições (professor e aluno)
        return lessonRepository.findByTeacher_UserIdOrStudent_UserIdAndScheduledTimeAfter(userId, userId, now);
    }

    @Override
    public void notifyLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        User teacher = lesson.getTeacher();
        User student = lesson.getStudent();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        ZonedDateTime lessonTime = lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"));

        if (now.toLocalDate().equals(lessonTime.toLocalDate())) {
            String lessonDetails = "Aula agendada para hoje às " + lessonTime.toLocalTime() + " com " + teacher.getUsername();
            emailService.sendLessonNotification(teacher.getEmail(), lessonDetails);
            emailService.sendPlatformNotification("Lembrete de aula para " + teacher.getUsername() + ": " + lessonDetails);
            Notification teacherNotification = new Notification();
            teacherNotification.setUser(teacher);
            teacherNotification.setMessage(lessonDetails);
            teacherNotification.setSentAt(Instant.now());
            teacherNotification.setRead(false);
            notificationRepository.save(teacherNotification);

            emailService.sendLessonNotification(student.getEmail(), lessonDetails);
            emailService.sendPlatformNotification("Lembrete de aula para " + student.getUsername() + ": " + lessonDetails);
            Notification studentNotification = new Notification();
            studentNotification.setUser(student);
            studentNotification.setMessage(lessonDetails);
            studentNotification.setSentAt(Instant.now());
            studentNotification.setRead(false);
            notificationRepository.save(studentNotification);
        }

        if (now.toLocalTime().isAfter(lessonTime.toLocalTime().minusMinutes(15)) && now.toLocalTime().isBefore(lessonTime.toLocalTime().plusMinutes(15))) {
            String lessonDetails = "A aula com " + teacher.getUsername() + " começa agora às " + lessonTime.toLocalTime();
            emailService.sendLessonNotification(teacher.getEmail(), lessonDetails);
            emailService.sendPlatformNotification("Aula começando para " + teacher.getUsername() + ": " + lessonDetails);
            Notification teacherNotification = new Notification();
            teacherNotification.setUser(teacher);
            teacherNotification.setMessage(lessonDetails);
            teacherNotification.setSentAt(Instant.now());
            teacherNotification.setRead(false);
            notificationRepository.save(teacherNotification);

            emailService.sendLessonNotification(student.getEmail(), lessonDetails);
            emailService.sendPlatformNotification("Aula começando para " + student.getUsername() + ": " + lessonDetails);
            Notification studentNotification = new Notification();
            studentNotification.setUser(student);
            studentNotification.setMessage(lessonDetails);
            studentNotification.setSentAt(Instant.now());
            studentNotification.setRead(false);
            notificationRepository.save(studentNotification);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Executa todos os dias à meia-noite
    public void dailyNotificationCheck() {
        List<Lesson> lessons = lessonRepository.findAll();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        for (Lesson lesson : lessons) {
            ZonedDateTime lessonTime = lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"));
            if (now.toLocalDate().equals(lessonTime.toLocalDate())) {
                notifyLesson(lesson.getLessonId());
            }
        }
    }
}