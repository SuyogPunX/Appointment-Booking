package com.spring.appointment.records;

import java.time.LocalDateTime;

public record AppointmentConfirmResponse(
        String status,
        String message,
        Long appointmentId,
        Long customerId,
        Long providerId,
        LocalDateTime appointmentTime
) {
    public AppointmentConfirmResponse(Long appointmentId, Long customerId, Long providerId, LocalDateTime appointmentTime) {
        this("success", "Appointment Confirmed successfully", appointmentId, customerId, providerId, appointmentTime);
    }

    public AppointmentConfirmResponse(String message) {
        this("error", message, null, null, null, null);
    }
}
