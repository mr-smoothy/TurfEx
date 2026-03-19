package com.turfexplorer.dto;

import lombok.Data;

@Data
public class PaymentInitResponse {
    private String transactionId;
    private String gatewayPageURL;
    private String message;
    private Double totalAmount;
    private Double paidAmount;
    private Boolean isPartial;

    public PaymentInitResponse(String transactionId, String gatewayPageURL, String message) {
        this(transactionId, gatewayPageURL, message, null, null, null);
    }

    public PaymentInitResponse(String transactionId, String gatewayPageURL, String message,
                               Double totalAmount, Double paidAmount, Boolean isPartial) {
        this.transactionId = transactionId;
        this.gatewayPageURL = gatewayPageURL;
        this.message = message;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.isPartial = isPartial;
    }
}
