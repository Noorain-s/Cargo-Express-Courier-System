package com.cargo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 20)
    private String mode; // CASH, CARD, UPI, ONLINE

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false, length = 20)
    private String paymentStatus; // PAID, PENDING, FAILED

    @Column(length = 64)
    private String razorpayOrderId;

    @Column(length = 64)
    private String razorpayPaymentId;
}
