package com.turfx.dto;

import com.turfx.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingRegistration {

	private String fullName;
    private String mobileNumber;
    private String email;
    private String password;
    private Role role;
    private String otp;
    private long expiryTime;
}
