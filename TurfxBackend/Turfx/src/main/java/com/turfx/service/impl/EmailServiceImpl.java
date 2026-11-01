
package com.turfx.service.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.turfx.service.EmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService 
{

    private final JavaMailSender javaMailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otp)
    {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Turfx - Email Verification OTP");

        message.setText(
                "Hello,\n\n"
                + "Your Turfx registration OTP is: " + otp + "\n\n"
                + "This OTP is valid for 5 minutes.\n\n"
                + "If you did not request this OTP, please ignore this email.\n\n"
                + "Regards,\n"
                + "Turfx Team"
        );

        javaMailSender.send(message);
    }
}
