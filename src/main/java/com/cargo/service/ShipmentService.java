package com.cargo.service;

import com.cargo.entity.Shipment;
import com.cargo.enums.PackageType;
import com.cargo.enums.ShipmentStatus;
import com.cargo.repository.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShipmentService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    // ===== Rate configuration (₹ per kg) =====
    private static final double DOCUMENT_RATE = 20.0;
    private static final double PARCEL_RATE = 50.0;
    private static final double FRAGILE_RATE = 80.0;
    private static final double FRAGILE_HANDLING_FEE = 30.0;

    /**
     * Calculates shipment cost based on package type and weight.
     * Document -> weight * 20
     * Parcel   -> weight * 50
     * Fragile  -> (weight * 80) + 30 flat handling fee
     */
    public double calculateCost(PackageType type, double weightKg) {
        if (weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
        switch (type) {
            case DOCUMENT:
                return weightKg * DOCUMENT_RATE;
            case PARCEL:
                return weightKg * PARCEL_RATE;
            case FRAGILE:
                return (weightKg * FRAGILE_RATE) + FRAGILE_HANDLING_FEE;
            default:
                throw new IllegalArgumentException("Unknown package type: " + type);
        }
    }

    // Books a new shipment: auto-calculates cost and sets status=BOOKED
    public Shipment bookShipment(Shipment shipment) {
        double cost = calculateCost(shipment.getPackageType(), shipment.getWeightKg());
        shipment.setCost(cost);
        shipment.setStatus(ShipmentStatus.BOOKED);
        shipment.setBookingDate(LocalDateTime.now());
        return shipmentRepository.save(shipment);
    }

    // Updates delivery status (used by Staff/Admin on tracking page)
    public Shipment updateStatus(Long shipmentId, ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
        shipment.setStatus(newStatus);
        return shipmentRepository.save(shipment);
    }

    // Assigns a staff member to handle a shipment
    public Shipment assignStaff(Long shipmentId, com.cargo.entity.User staff) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
        shipment.setHandledBy(staff);
        return shipmentRepository.save(shipment);
    }

    public Optional<Shipment> getShipmentById(Long id) {
        return shipmentRepository.findById(id);
    }

    public List<Shipment> getShipmentsByCustomer(Long customerId) {
        return shipmentRepository.findBySenderId(customerId);
    }

    public List<Shipment> getShipmentsByStatus(ShipmentStatus status) {
        return shipmentRepository.findByStatus(status);
    }

    public List<Shipment> getShipmentsByStaff(Long staffUserId) {
        return shipmentRepository.findByHandledById(staffUserId);
    }

    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    // ===== Reports =====
    public long countByStatus(ShipmentStatus status) {
        return shipmentRepository.countByStatus(status);
    }

    public double getTotalRevenue() {
        return shipmentRepository.findAll()
                .stream()
                .filter(s -> s.getStatus() != ShipmentStatus.CANCELLED)
                .mapToDouble(Shipment::getCost)
                .sum();
    }

    public void cancelShipment(Long shipmentId) {
        updateStatus(shipmentId, ShipmentStatus.CANCELLED);
    }
}
