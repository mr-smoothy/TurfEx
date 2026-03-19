package com.turfexplorer.repository;

import com.turfexplorer.entity.Payment;
import com.turfexplorer.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findAllByBookingIdOrderByCreatedAtDesc(Long bookingId);

    boolean existsByTransactionId(String transactionId);

    Optional<Payment> findFirstByUserIdAndSlotIdAndBookingDateAndStatusInOrderByCreatedAtDesc(
            Long userId,
            Long slotId,
            LocalDate bookingDate,
            List<PaymentStatus> statuses
    );

        Optional<Payment> findFirstBySlotIdAndBookingDateAndStatusInOrderByCreatedAtDesc(
            Long slotId,
            LocalDate bookingDate,
            List<PaymentStatus> statuses
        );

    List<Payment> findByStatus(PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("""
        SELECT COALESCE(COUNT(p), 0), COALESCE(SUM(p.paidAmount), 0)
        FROM Payment p
        JOIN Turf t ON t.id = p.turfId
        WHERE t.ownerId = :ownerId AND p.status IN :statuses
        """)
    Object[] getOwnerEarningsSummary(@Param("ownerId") Long ownerId, @Param("statuses") List<PaymentStatus> statuses);
}