package com.spring.appointment.records;

import java.time.LocalDateTime;

public record AppointmentBookingResponse(
        String status,
        String message,
        Long appointmentId,
        Long customerId,
        Long providerId,
        LocalDateTime appointmentTime
) {

    // Constructor for successful booking
    public AppointmentBookingResponse(Long appointmentId,Long customerId, Long providerId, LocalDateTime appointmentTime) {
        this("success", "Booked successfully",appointmentId,customerId, providerId, appointmentTime);
    }


    // Constructor for error cases
    public AppointmentBookingResponse(String message) {
        this("error", message,null, null, null, null);
    }



}