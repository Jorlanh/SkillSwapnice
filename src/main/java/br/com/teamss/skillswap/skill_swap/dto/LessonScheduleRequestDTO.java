package br.com.teamss.skillswap.skill_swap.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // ADICIONADO
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public class LessonScheduleRequestDTO {
    @NotNull(message = "teacherId não pode ser nulo")
    @JsonProperty("teacherId") // ADICIONADO
    private UUID teacherId;

    @NotNull(message = "studentId não pode ser nulo")
    @JsonProperty("studentId") // ADICIONADO
    private UUID studentId;

    @NotNull(message = "scheduledTime não pode ser nulo")
    @JsonProperty("scheduledTime") // ADICIONADO
    private Instant scheduledTime;

    // Getters e Setters
    public UUID getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(UUID teacherId) {
        this.teacherId = teacherId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
}