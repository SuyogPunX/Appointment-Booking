package com.spring.appointment.records;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AppointmentBookingRequest(
        @NotNull(message = "Appointment time is required")
        @Future(message = "Appointment time must be in the future")
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime appointmentTime
) {

}