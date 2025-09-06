package com.spring.appointment.records;


import com.spring.appointment.model.Provider;
import java.time.LocalDate;
import java.util.List;

public record ProviderSlotsResponse(
        Long providerId,
        String providerName,
        String serviceType,
        LocalDate date,
        List<TimeSlot> availableSlots,
        String error
) {
    // Success constructor with provider object
    public static ProviderSlotsResponse of(Provider provider, LocalDate date,
                                           List<TimeSlot> availableSlots) {
        return new ProviderSlotsResponse(
                provider.getProviderId(),
                provider.getUser().getName(),
                provider.getServiceType(),
                date,
                availableSlots,
                null
        );
    }

    // Error constructor
    public static ProviderSlotsResponse error(Long providerId, LocalDate date, String errorMessage) {
        return new ProviderSlotsResponse(
                providerId,
                null,
                null,
                date,
                null,
                errorMessage
        );
    }

    // Alternative success constructor without provider object
    public static ProviderSlotsResponse success(Long providerId, String providerName,
                                                String serviceType, LocalDate date,
                                                List<TimeSlot> availableSlots) {
        return new ProviderSlotsResponse(
                providerId,
                providerName,
                serviceType,
                date,
                availableSlots,
                null
        );
    }
}
