package com.spring.appointment.records;

import java.time.LocalDateTime;

public record TimeSlot(
        LocalDateTime startTime,
        int durationMinutes

) {
    // Factory method for regular time slots
    public static TimeSlot regular(LocalDateTime startTime, int durationMinutes) {
        return new TimeSlot(startTime, durationMinutes);
    }

}