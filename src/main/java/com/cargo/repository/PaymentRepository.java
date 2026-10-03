package com.cargo.repository;

import com.cargo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Get the invoice/payment for a specific shipment
    Optional<Payment> findByShipmentId(Long shipmentId);
}
