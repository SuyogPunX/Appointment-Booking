package com.spring.appointment.service;

import com.spring.appointment.model.Appointment;
import com.spring.appointment.model.Notification;
import com.spring.appointment.model.User;
import com.spring.appointment.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    // Notification helper methods
     void createBookingNotifications(Appointment appointment) {
        // Notification for customer
        Notification customerNotification = new Notification();
        customerNotification.setUser(appointment.getCustomer());
        customerNotification.setMessage(String.format(
                "Appointment booked with %s on %s. Status: PENDING",
                appointment.getProvider().getUser().getName(),
                appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
        ));
        notificationRepository.save(customerNotification);

        // Notification for provider
        Notification providerNotification = new Notification();
        providerNotification.setUser(appointment.getProvider().getUser());
        providerNotification.setMessage(String.format(
                "New appointment request from %s on %s",
                appointment.getCustomer().getName(),
                appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
        ));
        notificationRepository.save(providerNotification);
    }

    void createConfirmationNotifications(Appointment appointment) {
        // Notification for customer
        Notification customerNotification = new Notification();
        customerNotification.setUser(appointment.getCustomer());
        customerNotification.setMessage(String.format(
                "Your appointment with %s on %s has been CONFIRMED",
                appointment.getProvider().getUser().getName(),
                appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
        ));
        notificationRepository.save(customerNotification);

        // Notification for provider
        Notification providerNotification = new Notification();
        providerNotification.setUser(appointment.getProvider().getUser());
        providerNotification.setMessage(String.format(
                "Appointment with %s on %s has been confirmed",
                appointment.getCustomer().getName(),
                appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
        ));
        notificationRepository.save(providerNotification);
    }

    void createCancellationNotifications(Appointment appointment, User cancelledBy) {
        String cancelledByName = cancelledBy.getName();
        boolean cancelledByCustomer = cancelledBy.getUserId().equals(appointment.getCustomer().getUserId());

        // Notification for the other party
        User otherParty = cancelledByCustomer ? appointment.getProvider().getUser() : appointment.getCustomer();
        Notification otherPartyNotification = new Notification();
        otherPartyNotification.setUser(otherParty);
        otherPartyNotification.setMessage(String.format(
                "Appointment on %s has been CANCELLED by %s",
                appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")),
                cancelledByName
        ));
        notificationRepository.save(otherPartyNotification);

        // Notification for the person who cancelled
        Notification cancellerNotification = new Notification();
        cancellerNotification.setUser(cancelledBy);
        cancellerNotification.setMessage(String.format(
                "You have CANCELLED your appointment on %s",
                appointment.getAppointmentTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
        ));
        notificationRepository.save(cancellerNotification);
    }



}