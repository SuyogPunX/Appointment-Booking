package com.spring.appointment.service;

import com.spring.appointment.records.AuthenticateRequest;
import com.spring.appointment.records.AuthenticationResponse;
import com.spring.appointment.records.UserRegisterRequest;
import com.spring.appointment.enums.UserRole;
import com.spring.appointment.enums.UserStatus;
import com.spring.appointment.model.Provider;
import com.spring.appointment.model.User;
import com.spring.appointment.model.Wallet;
import com.spring.appointment.repository.ProviderRepository;
import com.spring.appointment.repository.UserRepository;
import com.spring.appointment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {
    private final UserRepository repository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletRepository walletRepository;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(UserRegisterRequest request) {
        log.info("Registering new customer with email: {}", request.email());

        // Check if email already exists
        if (repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Set default role if not provided
        UserRole role;
        if (request.role() == null || request.role().trim().isEmpty()) {
            role = UserRole.CUSTOMER; // Default role
        } else {
            try {
                role = UserRole.valueOf(request.role().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.role());
            }
        }

        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        repository.save(user);

        // Create wallet for every user
        Wallet wallet = new Wallet(user);
        walletRepository.save(wallet);

        Long providerId = null;
        if (UserRole.PROVIDER.equals(user.getRole())) {
            Provider provider = new Provider();
            provider.setUser(user);
            provider.setServiceType(request.serviceType());
            provider.setBio(request.bio());
            Provider savedProvider = providerRepository.save(provider);
            providerId = savedProvider.getProviderId();
        }

        var jwtToken = jwtService.generateToken(user);
        log.info("User registered successfully with ID: {}", user.getUserId());

        return new AuthenticationResponse(
                jwtToken,
                user.getRole().name(),
                user.getUserId(),
                providerId,
                null
        );
    }

    public AuthenticationResponse authenticate(AuthenticateRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password())
        );


        User user = repository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));


        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (UserStatus.BLOCKED.equals(user.getStatus())) {
            throw new RuntimeException("Account is blocked. Contact administrator");
        }

        var jwtToken = jwtService.generateToken(user);

        return new AuthenticationResponse(
                jwtToken,
                user.getRole().name(),
                user.getUserId(),
                null,  // providerId is null for authentication
                null   // message is null
        );
    }
}