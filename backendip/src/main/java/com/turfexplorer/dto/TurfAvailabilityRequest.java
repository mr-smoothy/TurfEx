package com.turfexplorer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TurfAvailabilityRequest {

    @NotNull(message = "Availability is required")
    private Boolean available;
}