package com.spring.appointment.service;

import com.spring.appointment.enums.TransactionStatus;
import com.spring.appointment.enums.TransactionType;
import com.spring.appointment.model.Transaction;
import com.spring.appointment.model.User;
import com.spring.appointment.model.Wallet;
import com.spring.appointment.repository.TransactionRepository;
import com.spring.appointment.repository.UserRepository;
import com.spring.appointment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@Transactional
@RequiredArgsConstructor
public class WalletService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public double getWalletBalance(long userId) {
        Wallet userWallet=walletRepository.findByUserUserId(userId).orElseThrow(()->new IllegalArgumentException("Wallet not found for user id: "+userId));
        return userWallet.getBalance();
    }


    public double deposit(long userId, double amount) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        // Find wallet by USER ID
        Wallet wallet = walletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user id: " + userId));

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Update wallet balance
        double newBalance = wallet.getBalance() + amount;
        wallet.setBalance(newBalance);
        wallet.setLastUpdated(LocalDateTime.now());
        walletRepository.save(wallet);

        return newBalance;
    }



    }

