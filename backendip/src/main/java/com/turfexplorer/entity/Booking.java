package com.turfexplorer.entity;

import com.turfexplorer.enums.BookingStatus;
import com.turfexplorer.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "turf_id", nullable = false)
    private Long turfId;
    
    @Column(name = "slot_id", nullable = false)
    private Long slotId;
    
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "total_amount", columnDefinition = "DOUBLE DEFAULT 0")
    private Double totalAmount = 0.0;

    @Column(name = "paid_amount", columnDefinition = "DOUBLE DEFAULT 0")
    private Double paidAmount = 0.0;

    @Column(name = "due_amount", columnDefinition = "DOUBLE DEFAULT 0")
    private Double dueAmount = 0.0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
