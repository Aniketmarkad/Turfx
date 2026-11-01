package com.turfx.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turfx.dto.LoginRequest;
import com.turfx.dto.LoginResponse;
import com.turfx.dto.RegisterRequest;
import com.turfx.dto.VerifyEmailOtpRequest;
import com.turfx.service.IUserService;
import com.turfx.dto.ResendOtpRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    // 1. Register user and send OTP
    @PostMapping("/register")
    public String registerUser(@Valid @RequestBody RegisterRequest request) 
    {

        return userService.registerUser(request);
    }

    // 2. Verify email OTP
    @PostMapping("/verify-email")
    public String verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request)
    {

        return userService.verifyEmailOtp(request);
    }
    
    // 3. Login user
    @PostMapping("/login")
    public LoginResponse loginUser(@Valid @RequestBody LoginRequest request)
    {
        return userService.loginUser(request);
    }

    // 4. Resend OTP
    @PostMapping("/resend-otp")
    public String resendOtp(@Valid @RequestBody ResendOtpRequest request)
    {

        return userService.resendOtp(request);
    }
}