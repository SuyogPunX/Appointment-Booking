package com.spring.appointment.records;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletDepositRequest(
        @NotNull
        @Min(value = 0, message = "Deposit amount must be non-negative")
        double amount
) {}