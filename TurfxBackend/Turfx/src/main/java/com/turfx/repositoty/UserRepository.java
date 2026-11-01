package com.turfx.repositoty;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.turfx.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> 
{

	   Optional<User> findByEmail(String email);

	    Optional<User> findByMobileNumber(String mobileNumber);

	    boolean existsByEmail(String email);

	    boolean existsByMobileNumber(String mobileNumber);
}
