package com.turfexplorer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRequest {
    private String message;
    private String sessionId;
    private String userRole;
    private String userName;
    private Double latitude;
    private Double longitude;
}
