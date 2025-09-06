package com.spring.appointment.controller;

import com.spring.appointment.records.AuthenticateRequest;
import com.spring.appointment.records.AuthenticationResponse;
import com.spring.appointment.records.UserRegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.spring.appointment.service.AuthenticationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody UserRegisterRequest request) throws BadRequestException {
        try {
            AuthenticationResponse response = service.register(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticateRequest request) throws BadRequestException {
        try{
            AuthenticationResponse response = service.authenticate(request);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }catch (IllegalArgumentException e){
            throw new BadRequestException(e.getMessage());
        }

    }

}
