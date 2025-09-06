package com.spring.appointment.repository;


import com.spring.appointment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface AppointmentRepository extends JpaRepository<Appointment, Long> {


    // Get all booked times for a provider on a specific date
    // Repository method - get booked slots more efficiently
    @Query("SELECT a.appointmentTime FROM Appointment a WHERE a.provider.providerId = :providerId " +
            "AND a.appointmentTime >= :startDate AND a.appointmentTime < :endDate " +
            "AND a.status != 'CANCELLED'")
    List<LocalDateTime> findBookedTimesByProviderAndDate(
            @Param("providerId") Long providerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Get appointments for provider
    List<Appointment> findByProviderProviderIdOrderByAppointmentTimeDesc(Long providerId);

    // Get appointments for customer
    List<Appointment> findByCustomerUserIdOrderByAppointmentTimeDesc(Long customerId);

    // Check if provider has active appointment at given time
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Appointment a " +
            "WHERE a.provider.providerId = :providerId " +
            "AND a.appointmentTime = :appointmentTime " +
            "AND a.status IN ('PENDING', 'CONFIRMED')")
    boolean existsActiveAppointmentAtTime(
            @Param("providerId") Long providerId,
            @Param("appointmentTime") LocalDateTime appointmentTime
    );

    // Find existing appointment at same time (for handling cancelled slots)
    Optional<Appointment> findByProviderProviderIdAndAppointmentTime(
            Long providerId,
            LocalDateTime appointmentTime
    );





}
