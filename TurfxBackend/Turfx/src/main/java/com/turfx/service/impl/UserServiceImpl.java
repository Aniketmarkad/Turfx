package com.turfx.service.impl;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.turfx.dto.LoginRequest;
import com.turfx.dto.LoginResponse;
import com.turfx.dto.PendingRegistration;
import com.turfx.dto.RegisterRequest;
import com.turfx.dto.VerifyEmailOtpRequest;
import com.turfx.entity.User;
import com.turfx.repositoty.UserRepository;
import com.turfx.service.EmailService;
import com.turfx.service.IUserService;
import com.turfx.service.PendingRegistrationStore;
import com.turfx.dto.ResendOtpRequest;

import com.turfx.exception.ResourceNotFoundException;
import com.turfx.exception.DuplicateResourceException;
import com.turfx.exception.InvalidOtpException;
import com.turfx.exception.OtpExpiredException;
import com.turfx.exception.InvalidCredentialsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService
{

	private final UserRepository userRepository;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final EmailService emailService;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public String registerUser(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        String mobileNumber = request.getMobileNumber().trim();

        // 1. Check whether email already exists
        if (userRepository.existsByEmail(email)) 
        {
           throw new DuplicateResourceException("Email is already registered");
        }

        // 2. Check whether mobile number already exists
        if (userRepository.existsByMobileNumber(mobileNumber)) 
        {
            throw new DuplicateResourceException("Mobile number is already registered");
        }

        // 3. Generate a 6-digit OTP
        String otp = String.valueOf(
                100000 + secureRandom.nextInt(900000)
        );

        // 4. Set OTP expiry time (5 minutes)
        long expiryTime = System.currentTimeMillis() + (5 * 60 * 1000L);

        // 5. Create temporary registration object
        PendingRegistration pendingRegistration = new PendingRegistration(
                request.getFullName(),
                mobileNumber,
                email,
                request.getPassword(),
                request.getRole(),
                otp,
                expiryTime
        );

        // 6. Store registration temporarily
        pendingRegistrationStore.save(email, pendingRegistration);

        // 7. Send OTP to registered email
        emailService.sendOtpEmail(email, otp);

        return "OTP sent successfully. Please verify your email within 5 minutes.";
    }

    
    @Override
    public String verifyEmailOtp(VerifyEmailOtpRequest request) 
    {

        String email = request.getEmail().trim().toLowerCase();

        // 1. Find pending registration using email
        PendingRegistration pendingRegistration =
                pendingRegistrationStore.get(email);

        // 2. Check whether registration exists
        if (pendingRegistration == null)
        {
            throw new ResourceNotFoundException("No pending registration found");
        }

        // 3. Check OTP expiry
        if (System.currentTimeMillis() > pendingRegistration.getExpiryTime()) 
        {
            pendingRegistrationStore.remove(email);
            throw new OtpExpiredException("OTP has expired. Please register again.");
        }

        // 4. Verify entered OTP
        if (!pendingRegistration.getOtp().equals(request.getOtp())) 
        {
           throw new InvalidOtpException("Invalid OTP");
        }

        // 5. Create User entity
        User user = new User();

        user.setFullName(pendingRegistration.getFullName());
        user.setMobileNumber(pendingRegistration.getMobileNumber());
        user.setEmail(pendingRegistration.getEmail());
        user.setPassword(pendingRegistration.getPassword());
        user.setRole(pendingRegistration.getRole());
        user.setEmailVerified(true);

        // 6. Save verified user in MySQL
        userRepository.save(user);

        // 7. Remove temporary registration
        pendingRegistrationStore.remove(email);

        return "Email verified and registration completed successfully!";
    }
    
    
    @Override
    public LoginResponse loginUser(LoginRequest request) 
    {

        String emailOrMobile = request.getEmailOrMobile().trim();
        String password = request.getPassword();

        User user;

        // 1. Find user by email or mobile number
        if (emailOrMobile.contains("@")) 
        {

            user = userRepository.findByEmail(emailOrMobile.toLowerCase())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User not found"));

        } else {

            user = userRepository.findByMobileNumber(emailOrMobile)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("User not found"));
        }

        // 2. Verify password
        if (!user.getPassword().equals(password)) 
        {
           throw new InvalidCredentialsException("Invalid password");
        }

        // 3. Create structured login response
        return new LoginResponse(
                "Login successful",
                user.getFullName(),
                user.getRole().name()
        );
    }


	@Override
    public String resendOtp(ResendOtpRequest request)
    {

        String email = request.getEmail().trim().toLowerCase();

        // 1. Find existing pending registration
        PendingRegistration pendingRegistration =
                pendingRegistrationStore.get(email);

        if (pendingRegistration == null) 
        {
            throw new ResourceNotFoundException("No pending registration found");
        }

        // 2. Generate a new 6-digit OTP
        String otp = String.valueOf(
                100000 + secureRandom.nextInt(900000)
        );

        // 3. Set new OTP expiry time (5 minutes)
        long expiryTime =
                System.currentTimeMillis() + (5 * 60 * 1000L);

        // 4. Update OTP and expiry time
        pendingRegistration.setOtp(otp);
        pendingRegistration.setExpiryTime(expiryTime);

        // 5. Save updated pending registration
        pendingRegistrationStore.save(email, pendingRegistration);

        // 6. Send new OTP
        emailService.sendOtpEmail(email, otp);

        return "New OTP sent successfully. Please verify your email within 5 minutes.";
    }
    
}