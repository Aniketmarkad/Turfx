package com.turfx.service.impl;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.turfx.service.IOtpService;

@Service
public class OtpServiceImpl implements IOtpService 
{

    private static final int OTP_LENGTH = 6;
    private static final int OTP_BOUND = 900000;
    private static final int OTP_MIN = 100000;
    private static final long OTP_VALIDITY_MILLIS = 5 * 60 * 1000L;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOtp() 
    {

        int otp = OTP_MIN + secureRandom.nextInt(OTP_BOUND);

        return String.valueOf(otp);
    }

    @Override
    public long calculateExpiryTime() 
    {

        return System.currentTimeMillis() + OTP_VALIDITY_MILLIS;
    }
}