package com.turfx.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.turfx.dto.PendingRegistration;

@Component
public class PendingRegistrationStore
{

	 private final Map<String, PendingRegistration> registrations =
	            new ConcurrentHashMap<>();

	    public void save(String email, PendingRegistration registration)
	    {
	        registrations.put(email, registration);
	    }

	    public PendingRegistration get(String email)
	    {
	        return registrations.get(email);
	    }

	    public void remove(String email)
	    {
	        registrations.remove(email);
	    }
	    
	    public boolean isPresent(String email) {
	    	return registrations.containsKey(email);
	    }
}
