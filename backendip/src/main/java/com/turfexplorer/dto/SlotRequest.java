package com.turfexplorer.dto;

import com.turfexplorer.enums.SlotStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalTime;

@Data
public class SlotRequest {
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;

    // Optional for updates; defaults to AVAILABLE when omitted on create.
    private SlotStatus status;
}
