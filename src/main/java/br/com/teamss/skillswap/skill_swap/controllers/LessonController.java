package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.services.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid; // ADICIONADO
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lesson")
public class LessonController {

    @Autowired
    private LessonService lessonService;

    @PostMapping("/schedule")
    public Lesson scheduleLesson(@Valid @RequestBody LessonScheduleRequestDTO lessonRequest) { // ADICIONADO @Valid
        System.out.println("DTO recebido no controlador: " + lessonRequest); // ADICIONADO LOG
        return lessonService.scheduleLesson(lessonRequest);
    }

    @GetMapping("/upcoming/{userId}")
    public List<Lesson> getUpcomingLessons(@PathVariable UUID userId) {
        return lessonService.getUpcomingLessons(userId);
    }

    @PostMapping("/notify/{lessonId}")
    public void notifyLesson(@PathVariable Long lessonId) {
        lessonService.notifyLesson(lessonId);
    }
}