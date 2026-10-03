package com.cargo.repository;

import com.cargo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Find the Customer profile linked to a given User login (by user id)
    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByPhone(String phone);
}
