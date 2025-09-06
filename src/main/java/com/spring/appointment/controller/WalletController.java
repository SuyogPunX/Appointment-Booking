package com.spring.appointment.controller;

import com.spring.appointment.model.User;
import com.spring.appointment.records.BalanceResponse;
import com.spring.appointment.records.DepositResponse;
import com.spring.appointment.records.WalletDepositRequest;
import com.spring.appointment.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            String email =user.getEmail();
            Long userId = user.getUserId();

            double walletBalance = walletService.getWalletBalance(userId);
            BalanceResponse response = new BalanceResponse(email, userId, walletBalance);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new BalanceResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(@Valid @RequestBody WalletDepositRequest request,
                                                   Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Long userId = user.getUserId();

            double newBalance = walletService.deposit(userId, request.amount());

            DepositResponse response = new DepositResponse(userId, request.amount(), newBalance);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new DepositResponse(e.getMessage()));
        }
    }
}
