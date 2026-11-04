package com.turfx.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.turfx.dto.LoginRequest;
import com.turfx.dto.LoginResponse;
import com.turfx.dto.RegisterRequest;
import com.turfx.dto.ResendOtpRequest;
import com.turfx.dto.VerifyEmailOtpRequest;
import com.turfx.entity.PendingRegistration;
import com.turfx.entity.User;
import com.turfx.exception.DuplicateResourceException;
import com.turfx.exception.InvalidCredentialsException;
import com.turfx.exception.InvalidOtpException;
import com.turfx.exception.OtpExpiredException;
import com.turfx.exception.ResourceNotFoundException;
import com.turfx.repositoty.PendingRegistrationRepository;
import com.turfx.repositoty.UserRepository;
import com.turfx.service.IEmailService;
import com.turfx.service.IOtpService;
import com.turfx.service.IUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService
{

	private final UserRepository userRepository;
	private final PendingRegistrationRepository pendingRegistrationRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final IOtpService otpService;
    
   
    //# 1 RegisterUser
   
    @Override
    public String registerUser(RegisterRequest request) 
    {

    	String email = normalizeEmail(request.getEmail());
        String mobileNumber = request.getMobileNumber().trim();

        // 1. Check whether email and mobile already exists in db 
        assertEmailAndMobileAvailable(email, mobileNumber);
        
       // 2. Check whether email already has a pending registration
        if (pendingRegistrationRepository.existsByEmail(email)) 
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
        PendingRegistration pendingRegistration = new PendingRegistration();

        pendingRegistration.setFullName(request.getFullName().trim());
        pendingRegistration.setMobileNumber(mobileNumber);
        pendingRegistration.setEmail(email);
        pendingRegistration.setPassword(encodedPassword);
        pendingRegistration.setRole(request.getRole());
        pendingRegistration.setOtp(otp);
        pendingRegistration.setExpiryTime(expiryTime);


        // 7. Store registration temporarily
        pendingRegistrationRepository.save(pendingRegistration);

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
                pendingRegistrationRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("No pending registration found")
                      );


        // 2. Check OTP expiry
        if (System.currentTimeMillis() > pendingRegistration.getExpiryTime()) 
        {
            throw new OtpExpiredException("OTP has expired. Please register again.");
        }

        // 3. Verify entered OTP
        boolean otpMatches = MessageDigest.isEqual(
                pendingRegistration.getOtp().getBytes(StandardCharsets.UTF_8),
                request.getOtp().getBytes(StandardCharsets.UTF_8)
        		);

        if (!otpMatches) 
        {
            throw new InvalidOtpException("Invalid OTP");
        }

        // 4. Create User entity
        User user = new User();

        user.setFullName(pendingRegistration.getFullName());
        user.setMobileNumber(pendingRegistration.getMobileNumber());
        user.setEmail(pendingRegistration.getEmail());
        user.setPassword(pendingRegistration.getPassword());
        user.setRole(pendingRegistration.getRole());
        user.setEmailVerified(true);

        // 5. Save verified user in MySQL
        userRepository.save(user);

        // 6. Remove temporary registration
        pendingRegistrationRepository.delete(pendingRegistration);
        
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
                pendingRegistrationRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("No pending registration found")
                                 
                        	);

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
        pendingRegistrationRepository.save(pendingRegistration);

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