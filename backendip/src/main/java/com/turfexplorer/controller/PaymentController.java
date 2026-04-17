package com.turfexplorer.controller;

import com.turfexplorer.dto.PaymentCreateSessionRequest;
import com.turfexplorer.exception.BadRequestException;
import com.turfexplorer.security.UserDetailsServiceImpl;
import com.turfexplorer.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/create-bkash-payment")
    public ResponseEntity<Map<String, Object>> createBkashPayment(
            @Valid @RequestBody PaymentCreateSessionRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("Authentication is required for payment");
        }
        if (userDetailsService.getUserByEmail(authentication.getName()) == null) {
            throw new BadRequestException("Authenticated user not found");
        }
        Long userId = userDetailsService.getUserByEmail(authentication.getName()).getId();
        return ResponseEntity.ok(paymentService.createBkashPayment(userId, request.getBookingId()));
    }

    @PostMapping("/execute-bkash-payment")
    public ResponseEntity<Map<String, Object>> executeBkashPayment(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("Authentication is required for payment");
        }
        if (userDetailsService.getUserByEmail(authentication.getName()) == null) {
            throw new BadRequestException("Authenticated user not found");
        }
        Long userId = userDetailsService.getUserByEmail(authentication.getName()).getId();
        String paymentId = request.get("paymentID");
        return ResponseEntity.ok(paymentService.executeBkashPayment(userId, paymentId));
    }
}
