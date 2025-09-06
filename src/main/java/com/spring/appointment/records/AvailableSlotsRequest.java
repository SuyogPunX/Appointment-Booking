package com.spring.appointment.records;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AvailableSlotsRequest(
        @NotNull
        @Future(message = "Date must be in the future")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate date
) {}