package com.turfexplorer.entity;

import com.turfexplorer.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "turf_id", nullable = false)
    private Long turfId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    // Backward compatibility for existing databases that still have old bKash columns.
    @Column(name = "bkash_payment_id")
    private String bkashPaymentId;

    // Backward compatibility for existing databases that still have old bKash columns.
    @Column(name = "merchant_invoice_number")
    private String merchantInvoiceNumber;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    // Backward compatibility for existing databases that still have old bKash columns.
    @Column(name = "trx_id")
    private String trxId;

    @Column(name = "validation_id")
    private String validationId;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "paid_amount")
    private Double paidAmount;

    @Column(name = "is_partial")
    private Boolean isPartial = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Lob
    @Column(name = "raw_init_response", columnDefinition = "LONGTEXT")
    private String rawInitResponse;

    // Backward compatibility for existing databases that still have old bKash columns.
    @Lob
    @Column(name = "raw_create_response", columnDefinition = "LONGTEXT")
    private String rawCreateResponse;

    @Lob
    @Column(name = "raw_validation_response", columnDefinition = "LONGTEXT")
    private String rawValidationResponse;

    // Backward compatibility for existing databases that still have old bKash columns.
    @Lob
    @Column(name = "raw_execute_response", columnDefinition = "LONGTEXT")
    private String rawExecuteResponse;

    // Backward compatibility for existing databases that still have old bKash columns.
    @Lob
    @Column(name = "raw_query_response", columnDefinition = "LONGTEXT")
    private String rawQueryResponse;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}