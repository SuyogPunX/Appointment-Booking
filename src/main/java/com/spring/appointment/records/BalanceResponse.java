package com.spring.appointment.records;

public record BalanceResponse(
        String status,
        String message,
        String email,
        Long userId,
        double balance
) {
    public BalanceResponse(String email, Long userId, double balance) {
        this("success", "Balance retrieved successfully", email, userId, balance);
    }

    public BalanceResponse( String message) {
        this("error", message, null, null, 0);
    }
}