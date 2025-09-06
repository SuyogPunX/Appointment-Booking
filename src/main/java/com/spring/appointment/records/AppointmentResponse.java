package com.spring.appointment.records;

import com.spring.appointment.enums.AppointmentStatus;
import com.spring.appointment.enums.PaymentStatus;
import com.spring.appointment.model.Appointment;

import java.time.LocalDateTime;

public record AppointmentResponse(
        String status,
        Long appointmentId,
        String customerName,
        String providerName,
        LocalDateTime appointmentTime,
        AppointmentStatus appointmentStatus,
        PaymentStatus paymentStatus,
        String error

) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                "success",
                appointment.getAppointmentId(),
                appointment.getCustomer().getName(),
                appointment.getProvider().getUser().getName(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.getPaymentStatus(),
                null
        );
    }

    public AppointmentResponse(String message) {
        this("error",null,null, null, null, null, null,message);
    }
}

