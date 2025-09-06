package com.spring.appointment.records;

public record DepositResponse(
        String status,
        String message,
        Long userId,
        double amountDeposited,
        double newBalance
) {
    public DepositResponse(Long userId, double amountDeposited, double newBalance) {
        this("success", "Deposit completed successfully", userId, amountDeposited, newBalance);
    }

    public DepositResponse(String message) {
        this("error", message, null, 0, 0);
    }
}