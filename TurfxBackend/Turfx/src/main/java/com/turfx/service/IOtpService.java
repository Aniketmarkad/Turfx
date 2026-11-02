package com.turfx.service;

public interface IOtpService 
{

    String generateOtp();

    long calculateExpiryTime();
}