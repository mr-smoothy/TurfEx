package com.turfexplorer.dto;

import lombok.Data;

@Data
public class PaymentCallbackResultResponse {
    private String status;
    private String message;
    private String transactionId;
    private Long bookingId;
    private Double totalAmount;
    private Double paidAmount;
    private Double dueAmount;
    private String paymentStatus;

    public PaymentCallbackResultResponse(String status, String message, String transactionId, Long bookingId) {
        this(status, message, transactionId, bookingId, null, null, null, null);
    }

    public PaymentCallbackResultResponse(String status, String message, String transactionId, Long bookingId,
                                         Double totalAmount, Double paidAmount, Double dueAmount, String paymentStatus) {
        this.status = status;
        this.message = message;
        this.transactionId = transactionId;
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.dueAmount = dueAmount;
        this.paymentStatus = paymentStatus;
    }
}
