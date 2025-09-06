package com.spring.appointment.repository;

import com.spring.appointment.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ProviderRepository extends JpaRepository<Provider, Long> {
        Optional<Provider> findByUserUserId(long userId);
}
