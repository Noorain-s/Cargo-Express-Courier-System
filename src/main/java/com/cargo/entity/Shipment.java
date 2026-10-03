package com.cargo.entity;

import com.cargo.enums.PackageType;
import com.cargo.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sender = the customer who booked the shipment
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private Customer sender;

    @Column(nullable = false, length = 100)
    private String receiverName;

    @Column(nullable = false, length = 15)
    private String receiverPhone;

    @Column(nullable = false, length = 255)
    private String receiverAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageType packageType;

    @Column(nullable = false)
    private Double weightKg;

    @Column(nullable = false)
    private Double cost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(nullable = false)
    private LocalDateTime bookingDate;

    // Staff member handling this shipment (nullable until assigned)
    @ManyToOne
    @JoinColumn(name = "handled_by_staff_id")
    private User handledBy;
}
