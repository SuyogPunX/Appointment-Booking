package com.spring.appointment.records;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthenticationResponse(
        String token,
        String role,
        Long userId,
        Long providerId,
        String message
) {
    // Convenience constructors
    public AuthenticationResponse(String token, String role, Long userId) {
        this(token, role, userId, null, null);
    }

    public AuthenticationResponse(String token, String role, Long userId, Long providerId) {
        this(token, role, userId, providerId, null);
    }

    public AuthenticationResponse(String message) {
        this(null, null, null, null, message);
    }
}