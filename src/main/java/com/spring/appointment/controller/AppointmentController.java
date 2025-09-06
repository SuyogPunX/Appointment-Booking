package com.spring.appointment.controller;

import com.spring.appointment.model.Appointment;
import com.spring.appointment.model.Notification;
import com.spring.appointment.model.Provider;

import com.spring.appointment.model.User;
import com.spring.appointment.records.*;

import com.spring.appointment.service.AppointmentService;
import com.spring.appointment.service.NotificationService;
import com.spring.appointment.service.ProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final ProviderService providerService;
    private final NotificationService notificationService;

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderListResponse>> getAllProviders() {
        try {
            List<Provider> providers = appointmentService.getAllProviders();

            List<ProviderListResponse> response = providers.stream()
                    .map(ProviderListResponse::convert)
                    .toList();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(emptyList());
        }
    }

    @PostMapping("providers/{providerId}/book")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentBookingResponse> bookAppointment(@PathVariable Long providerId,@Valid @RequestBody AppointmentBookingRequest request, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            long customerId = user.getUserId();

           Appointment appointment= appointmentService.bookAppointment(customerId, providerId,request);
           AppointmentBookingResponse response = new AppointmentBookingResponse(appointment.getAppointmentId(),customerId,providerId,request.appointmentTime());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AppointmentBookingResponse(e.getMessage()));
        }

    }

    @GetMapping("/allAppointments")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            long userId = user.getUserId();

            List<Appointment> appointments = appointmentService.getAppointmentsForUser(userId);

            List<AppointmentResponse> appointmentResponse = appointments.stream()
                    .map(AppointmentResponse::from)
                    .toList();

            return ResponseEntity.ok(appointmentResponse);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(emptyList());
        }
    }

    @PostMapping("/provider/{providerId}/available-slots")
    public ResponseEntity<ProviderSlotsResponse> getProviderAvailableSlots(
            @PathVariable Long providerId,
            @Valid @RequestBody AvailableSlotsRequest request) {

        try {
            // Validate input parameters
            if (providerId == null || providerId <= 0) {
                return ResponseEntity.badRequest()
                        .body(ProviderSlotsResponse.error(providerId, request.date(), "INVALID_PROVIDER_ID"));
            }

            // Get provider information
            Provider provider = providerService.getProviderById(providerId);
            if (provider == null) {
                return ResponseEntity.notFound().build();
            }

            // Get available time slots
            List<LocalDateTime> availableSlotTimes = appointmentService.getAvailableTimeSlots(providerId, request.date());

            // Convert to TimeSlot records (using default 30-minute duration)
            List<TimeSlot> availableSlots = availableSlotTimes.stream()
                    .map(dateTime -> TimeSlot.regular(dateTime, 30))
                    .toList();

            // Create response
            ProviderSlotsResponse response = ProviderSlotsResponse.of(
                    provider, request.date(), availableSlots
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ProviderSlotsResponse.error(providerId, request.date(), "INTERNAL_ERROR: " + e.getMessage()));
        }
    }




    @PostMapping("/{appointmentId}/confirm")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<AppointmentConfirmResponse> confirmAppointment(@PathVariable Long appointmentId,Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            long userId = user.getUserId();
            Appointment appointment =appointmentService.confirmAppointment(appointmentId,userId);

            AppointmentConfirmResponse response = new AppointmentConfirmResponse(
                    appointment.getAppointmentId(),
                    appointment.getCustomer().getUserId(),
                    appointment.getProvider().getProviderId(),
                    appointment.getAppointmentTime()
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AppointmentConfirmResponse(e.getMessage()));
        }

    }

    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long appointmentId, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            long userId = user.getUserId();

            Appointment appointment = appointmentService.cancelAppointment(appointmentId, userId);
            return ResponseEntity.ok(AppointmentResponse.from(appointment));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AppointmentResponse(e.getMessage()));
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<Notification> notifications = notificationService.getUserNotifications(user.getUserId());

            List<NotificationResponse> response = notifications.stream()
                    .map(NotificationResponse::from)
                    .toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(emptyList());
        }
    }


}
