package com.cargo.repository;

import com.cargo.entity.Shipment;
import com.cargo.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    // All shipments booked by a specific customer (for their tracking/history page)
    List<Shipment> findBySenderId(Long senderId);

    // All shipments with a given status (e.g., all "IN_TRANSIT" for staff dashboard)
    List<Shipment> findByStatus(ShipmentStatus status);

    // All shipments handled by a specific staff member
    List<Shipment> findByHandledById(Long staffUserId);

    // For reports: count shipments by status
    long countByStatus(ShipmentStatus status);
}
