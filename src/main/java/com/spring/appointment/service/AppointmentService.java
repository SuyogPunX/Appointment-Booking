package com.spring.appointment.service;

import com.spring.appointment.enums.*;
import com.spring.appointment.model.*;
import com.spring.appointment.records.AppointmentBookingRequest;
import com.spring.appointment.repository.*;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AppointmentRepository appointmentRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private static final double APPOINTMENT_FEE=50.0;

    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }

    public List<Appointment> getAppointmentsForUser(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == UserRole.PROVIDER) {
            Provider provider = providerRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
            return appointmentRepository.findByProviderProviderIdOrderByAppointmentTimeDesc(provider.getProviderId());
        } else if (user.getRole() == UserRole.CUSTOMER) {
            return appointmentRepository.findByCustomerUserIdOrderByAppointmentTimeDesc(userId);
        } else {
            throw new IllegalArgumentException("Invalid user role");
        }
    }


    public Appointment bookAppointment(long customerId, long providerId, AppointmentBookingRequest request) {
        // 1. VALIDATE APPOINTMENT TIME
        validateAppointmentTime(request.appointmentTime());

        // 2. FIND ENTITIES
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + customerId));

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found with id: " + providerId));

        Wallet customerWallet = walletRepository.findByUserUserId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user id: " + customerId));

        // 3. CHECK BALANCE
        if (customerWallet.getBalance() < APPOINTMENT_FEE) {
            throw new IllegalArgumentException("Insufficient balance in wallet! Fee required: " + APPOINTMENT_FEE);
        }

        // 4. CHECK FOR EXISTING APPOINTMENT AT SAME TIME
        Optional<Appointment> existingAppointment = appointmentRepository.findByProviderProviderIdAndAppointmentTime(
                providerId,
                request.appointmentTime()
        );

        if (existingAppointment.isPresent()) {
            Appointment existing = existingAppointment.get();

            if (existing.getStatus() == null) {
                log.error("Appointment {} has null status", existing.getAppointmentId());
                throw new IllegalArgumentException("Invalid appointment data. Please try again.");
            }

            if (existing.getStatus() == AppointmentStatus.CANCELLED) {
                // Validate reused appointment
                if (existing.getAppointmentTime().isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Cannot reuse past cancelled appointment.");
                }

                // REUSE the cancelled appointment
                existing.setCustomer(customer);
                existing.setStatus(AppointmentStatus.PENDING);
                existing.setPaymentStatus(PaymentStatus.UNPAID);
                existing.setUpdatedAt(LocalDateTime.now());

                Appointment savedAppointment = appointmentRepository.save(existing);
                log.info("Reused cancelled appointment: {}", existing.getAppointmentId());

                notificationService.createBookingNotifications(savedAppointment);
                return savedAppointment;
            } else {
                // Active appointment exists - slot is taken
                throw new IllegalArgumentException("This time slot is no longer available.");
            }
        }

        // 5. CREATE AND SAVE NEW APPOINTMENT
        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setProvider(provider);
        appointment.setAppointmentTime(request.appointmentTime());
        appointment.setStatus(AppointmentStatus.PENDING);

        try {
            Appointment savedAppointment = appointmentRepository.save(appointment);
            log.info("Appointment booked: {}", savedAppointment.getAppointmentId());

            notificationService.createBookingNotifications(savedAppointment);
            return savedAppointment;

        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate booking prevented by database constraint for provider {} at {}",
                    providerId, request.appointmentTime());

            // Check if someone just booked this slot
            boolean nowTaken = appointmentRepository.existsActiveAppointmentAtTime(
                    providerId,
                    request.appointmentTime()
            );

            if (nowTaken) {
                throw new IllegalArgumentException("This time slot was just taken. Please choose another time.");
            } else {
                throw new IllegalArgumentException("Unable to book appointment due to system error. Please try again.");
            }
        }
    }

    public Appointment confirmAppointment(long appointmentId,long userId) {
        Appointment appointment=appointmentRepository.findById(appointmentId).orElseThrow(()->new IllegalArgumentException("Appointment not found with id: "+appointmentId));

        Provider provider=providerRepository.findByUserUserId(userId).orElseThrow(()->new IllegalArgumentException("Provider not found with id: "+userId));

        if (!appointment.getProvider().getProviderId().equals(provider.getProviderId())) {
            throw new IllegalArgumentException("You can only confirm your own appointments");
        }

        // Additional check to prevent confirming past appointments
        if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot confirm past appointments");
        }

        if(appointment.getStatus()!=AppointmentStatus.PENDING){
            throw new IllegalArgumentException("Only pending appointments can be confirmed");
        }

        // Process payment when confirming
        Wallet customerWallet = appointment.getCustomer().getWallet();
        Wallet providerWallet = provider.getUser().getWallet();

        // Deduct from customer
        customerWallet.setBalance(customerWallet.getBalance() - APPOINTMENT_FEE);

        // Add to provider
        providerWallet.setBalance(providerWallet.getBalance() + APPOINTMENT_FEE);

        // Save wallets
        walletRepository.save(customerWallet);
        walletRepository.save(providerWallet);

        // Confirm appointment
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setPaymentStatus(PaymentStatus.PAID);

        // Create customer transaction
        Transaction customerTransaction = Transaction.builder()
                .wallet(customerWallet)
                .appointment(appointment)
                .amount(APPOINTMENT_FEE)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .build();

        // Create provider transaction
        Transaction providerTransaction = Transaction.builder()
                .wallet(providerWallet)
                .appointment(appointment)
                .amount(APPOINTMENT_FEE)
                .type(TransactionType.INCOME)
                .status(TransactionStatus.SUCCESS)
                .build();

        transactionRepository.saveAll(List.of(customerTransaction, providerTransaction));
        // Create notifications for confirmation
        notificationService.createConfirmationNotifications(appointment);

        return appointmentRepository.save(appointment);
    }

    public List<LocalDateTime> getAvailableTimeSlots(Long providerId, LocalDate date) {
        LocalDateTime startOfDay = date.atTime(9, 0);    // 9 AM
        LocalDateTime endOfDay = date.atTime(18, 0);     // 6 PM

        // Extend end time for query to include all relevant appointments
        LocalDateTime queryEndTime = endOfDay.plusMinutes(30);

        // Get all booked times for the day
        List<LocalDateTime> bookedTimes = appointmentRepository.findBookedTimesByProviderAndDate(
                providerId, startOfDay, queryEndTime
        );

        // Generate all possible time slots (every 30 minutes)
        List<LocalDateTime> allTimeSlots = new ArrayList<>();
        LocalDateTime currentSlot = startOfDay;

        while (currentSlot.isBefore(endOfDay)) {
            // Only add future time slots
            if (currentSlot.isAfter(LocalDateTime.now())) {
                allTimeSlots.add(currentSlot);
            }
            currentSlot = currentSlot.plusMinutes(30);  // 30-minute intervals
        }

        // Remove booked time slots
        Set<LocalDateTime> bookedSet = new HashSet<>(bookedTimes);

        return allTimeSlots.stream()
                .filter(slot -> !bookedSet.contains(slot))
                .collect(Collectors.toList());
    }


    public Appointment cancelAppointment(long appointmentId, long userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Check if user is either the customer or the provider
        boolean isCustomer = appointment.getCustomer().getUserId().equals(userId);
        boolean isProvider = appointment.getProvider().getUser().getUserId().equals(userId);

        if (!isCustomer && !isProvider) {
            throw new IllegalArgumentException("You can only cancel your own appointments");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Appointment is already cancelled");
        }

        // Additional check to prevent cancelling past appointments
        if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot cancel past appointments");
        }

        // Handle refunds based on appointment status
        if (appointment.getStatus() == AppointmentStatus.CONFIRMED &&
                appointment.getPaymentStatus() == PaymentStatus.PAID) {

            // Refund customer
            Wallet customerWallet = appointment.getCustomer().getWallet();
            customerWallet.setBalance(customerWallet.getBalance() + APPOINTMENT_FEE);

            // Deduct from provider
            Wallet providerWallet = appointment.getProvider().getUser().getWallet();
            providerWallet.setBalance(providerWallet.getBalance() - APPOINTMENT_FEE);

            // Save wallets
            walletRepository.saveAll(List.of(customerWallet, providerWallet));

            // Create refund transactions
            Transaction customerRefund = Transaction.builder()
                    .wallet(customerWallet)
                    .appointment(appointment)
                    .amount(APPOINTMENT_FEE)
                    .type(TransactionType.REFUND)
                    .status(TransactionStatus.SUCCESS)
                    .build();

            Transaction providerDeduction = Transaction.builder()
                    .wallet(providerWallet)
                    .appointment(appointment)
                    .amount(APPOINTMENT_FEE)
                    .type(TransactionType.DEDUCTION)
                    .status(TransactionStatus.SUCCESS)
                    .build();

            transactionRepository.saveAll(List.of(customerRefund, providerDeduction));
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment cancelledAppointment = appointmentRepository.save(appointment);

        // Create notifications for cancellation
        notificationService.createCancellationNotifications(cancelledAppointment, user);
        return cancelledAppointment;

    }

    private void validateAppointmentTime(LocalDateTime appointmentTime) {

        if (appointmentTime.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("Appointments must be booked at least 1 hour in advance");
        }

        // Only allow appointments on the hour or half hour
        int minute = appointmentTime.getMinute();
        if (minute != 0 && minute != 30) {
            throw new IllegalArgumentException("Appointments can only be booked on the hour or half-hour");
        }

        // Business hours validation
        int hour = appointmentTime.getHour();
        if (hour < 9 || hour >= 18) {
            throw new IllegalArgumentException("Appointments can only be booked between 9:00 AM and 6:00 PM");
        }

        // Weekend validation
        if (appointmentTime.getDayOfWeek().getValue() > 5) {
            throw new IllegalArgumentException("Appointments are not available on weekends");
        }

        // Prevent booking too far in advance
        if (appointmentTime.isAfter(LocalDateTime.now().plusDays(30))) {
            throw new IllegalArgumentException("Appointments can only be booked up to 30 days in advance");
        }

    }

}

