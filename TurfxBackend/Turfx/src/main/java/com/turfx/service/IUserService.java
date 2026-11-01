package com.turfx.service;

import com.turfx.dto.LoginRequest;
import com.turfx.dto.LoginResponse;
import com.turfx.dto.RegisterRequest;
import com.turfx.dto.VerifyEmailOtpRequest;
import com.turfx.dto.ResendOtpRequest;

public interface IUserService {

    String registerUser(RegisterRequest request);

    String verifyEmailOtp(VerifyEmailOtpRequest request);
    
    LoginResponse loginUser(LoginRequest request);
    
    String resendOtp(ResendOtpRequest request);
    
}