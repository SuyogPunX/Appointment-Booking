package com.spring.appointment.records;

import com.spring.appointment.model.Provider;

public record  ProviderListResponse(
        long providerId,
        String name,
        String serviceType,
        String bio

)  {
    public static ProviderListResponse convert(Provider provider) {
        return new ProviderListResponse(
                provider.getProviderId(),
                provider.getUser().getName(),
                provider.getServiceType(),
                provider.getBio()
        );

    }

}


