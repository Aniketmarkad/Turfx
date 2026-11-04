package com.turfx.repositoty;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.turfx.entity.PendingRegistration;

public interface PendingRegistrationRepository
        extends JpaRepository<PendingRegistration, Long> 
{

    Optional<PendingRegistration> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}