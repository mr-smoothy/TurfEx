package com.turfexplorer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OwnerEarningsResponse {
    private Long successfulPayments;
    private Double totalEarnings;
}