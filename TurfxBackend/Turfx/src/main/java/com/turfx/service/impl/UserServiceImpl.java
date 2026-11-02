package com.turfx.service.impl;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.turfx.dto.LoginRequest;
import com.turfx.dto.LoginResponse;
import com.turfx.dto.PendingRegistration;
import com.turfx.dto.RegisterRequest;
import com.turfx.dto.ResendOtpRequest;
import com.turfx.dto.VerifyEmailOtpRequest;
import com.turfx.entity.User;
import com.turfx.exception.DuplicateResourceException;
import com.turfx.exception.InvalidCredentialsException;
import com.turfx.exception.InvalidOtpException;
import com.turfx.exception.OtpExpiredException;
import com.turfx.exception.ResourceNotFoundException;
import com.turfx.repositoty.UserRepository;
import com.turfx.service.IEmailService;
import com.turfx.service.IUserService;
import com.turfx.service.IOtpService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService
{

	private final UserRepository userRepository;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final IOtpService otpService;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    //# 1 RegisterUser
   
    @Override
    public String registerUser(RegisterRequest request) 
    {

    	String email = normalizeEmail(request.getEmail());
        String mobileNumber = request.getMobileNumber().trim();

        // 1. Check whether email and mobile already exists
        assertEmailAndMobileAvailable(email, mobileNumber);
        
       // 2. Check whether email already has a pending registration
	     if (pendingRegistrationStore.get(email) != null) 
	     {
	         throw new DuplicateResourceException(
	                 "Registration is already pending for this email. Please verify OTP or request a resend."
	         );
	     }

        // 3. Generate a 6-digit OTP
        String otp = otpService.generateOtp();

        // 4. Set OTP expiry time (5 minutes)
        long expiryTime = otpService.calculateExpiryTime();

     
        // 5. Password Encoded 
        String encodedPassword = passwordEncoder.encode(request.getPassword());
       
        // 6. Create temporary registration object
        PendingRegistration pendingRegistration = new PendingRegistration(
                request.getFullName().trim(),
                mobileNumber,
                email,
                encodedPassword,
                request.getRole(),
                otp,
                expiryTime
        );

        // 7. Store registration temporarily
        pendingRegistrationStore.save(email, pendingRegistration);

        // 8. Send OTP to registered email
        emailService.sendOtpEmail(email, otp);

        return "OTP sent successfully. Please verify your email within 5 minutes.";
    }

    
    @Override
    public String verifyEmailOtp(VerifyEmailOtpRequest request) 
    {

    	String email = normalizeEmail(request.getEmail());

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
            throw new OtpExpiredException("OTP has expired. Please register again.");
        }

        // 4. Verify entered OTP
        boolean otpMatches = MessageDigest.isEqual(
                pendingRegistration.getOtp().getBytes(StandardCharsets.UTF_8),
                request.getOtp().getBytes(StandardCharsets.UTF_8)
        		);

        if (!otpMatches) 
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
        if (!passwordEncoder.matches(password, user.getPassword())) 
        {
            throw new InvalidCredentialsException("Invalid credentials");
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

		String email = normalizeEmail(request.getEmail());

        // 1. Find existing pending registration
        PendingRegistration pendingRegistration =
                pendingRegistrationStore.get(email);

        if (pendingRegistration == null) 
        {
            throw new ResourceNotFoundException("No pending registration found");
        }

        // 2. Generate a new 6-digit OTP
        String otp = otpService.generateOtp();
        
        // 3. Set new OTP expiry time (5 minutes)
        long expiryTime = otpService.calculateExpiryTime();
        
        // 4. Update OTP and expiry time
        pendingRegistration.setOtp(otp);
        pendingRegistration.setExpiryTime(expiryTime);

        // 5. Send new OTP
        emailService.sendOtpEmail(email, otp);
        
        // 6. Save updated pending registration
        pendingRegistrationStore.save(email, pendingRegistration);

        return "New OTP sent successfully. Please verify your email within 5 minutes.";
    }
	
	//============*=== __ Helper methods __ ===*============
	
	//#1 
	private String normalizeEmail(String email)
	{
	    return email.trim().toLowerCase();
	}
	
	//#2
	
	private void assertEmailAndMobileAvailable(String email, String mobileNumber)
	{

	    if (userRepository.existsByEmail(email)) 
	    {
	        throw new DuplicateResourceException("Email is already registered");
	    }

	    if (userRepository.existsByMobileNumber(mobileNumber)) 
	    {
	        throw new DuplicateResourceException("Mobile number is already registered");
	    }
	}
    
}