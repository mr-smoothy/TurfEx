package com.turfexplorer.controller;

import com.turfexplorer.dto.PaymentCallbackResultResponse;
import com.turfexplorer.dto.PaymentInitRequest;
import com.turfexplorer.dto.PaymentInitResponse;
import com.turfexplorer.entity.User;
import com.turfexplorer.security.UserDetailsServiceImpl;
import com.turfexplorer.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/init")
    public ResponseEntity<PaymentInitResponse> initPayment(
            @Valid @RequestBody PaymentInitRequest request,
            Authentication authentication) {
        User user = userDetailsService.getUserByEmail(authentication.getName());
        PaymentInitResponse response = paymentService.initPayment(
                user.getId(),
                user.getName(),
                user.getEmail(),
                request
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/init-remaining")
    public ResponseEntity<PaymentInitResponse> initRemainingPayment(
            @RequestParam("booking_id") Long bookingId,
            Authentication authentication) {
        User user = userDetailsService.getUserByEmail(authentication.getName());
        PaymentInitResponse response = paymentService.initRemainingPayment(
                user.getId(),
                user.getName(),
                user.getEmail(),
                bookingId
        );
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/success", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> paymentSuccess(@RequestParam Map<String, String> callbackData) {
        PaymentCallbackResultResponse result = paymentService.handleSuccessCallback(callbackData);
        URI redirectUri = paymentService.buildFrontendRedirectUrl(result.getStatus(), result.getTransactionId(), result.getBookingId());
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @RequestMapping(value = "/fail", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> paymentFail(@RequestParam Map<String, String> callbackData) {
        PaymentCallbackResultResponse result = paymentService.handleFailCallback(callbackData);
        URI redirectUri = paymentService.buildFrontendRedirectUrl(result.getStatus(), result.getTransactionId(), result.getBookingId());
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @RequestMapping(value = "/cancel", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> paymentCancel(@RequestParam Map<String, String> callbackData) {
        PaymentCallbackResultResponse result = paymentService.handleCancelCallback(callbackData);
        URI redirectUri = paymentService.buildFrontendRedirectUrl(result.getStatus(), result.getTransactionId(), result.getBookingId());
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @PostMapping("/verify-success")
    public ResponseEntity<PaymentCallbackResultResponse> verifySuccess(
            @RequestParam("tran_id") String transactionId,
            @RequestParam(value = "val_id", required = false) String valId,
            Authentication authentication) {
        Long userId = userDetailsService.getUserByEmail(authentication.getName()).getId();
        return ResponseEntity.ok(paymentService.verifySuccessForUser(userId, transactionId, valId));
    }

    @PostMapping("/verify-fail")
    public ResponseEntity<PaymentCallbackResultResponse> verifyFail(
            @RequestParam("tran_id") String transactionId,
            Authentication authentication) {
        Long userId = userDetailsService.getUserByEmail(authentication.getName()).getId();
        return ResponseEntity.ok(paymentService.verifyFailForUser(userId, transactionId));
    }

    @PostMapping("/verify-cancel")
    public ResponseEntity<PaymentCallbackResultResponse> verifyCancel(
            @RequestParam("tran_id") String transactionId,
            Authentication authentication) {
        Long userId = userDetailsService.getUserByEmail(authentication.getName()).getId();
        return ResponseEntity.ok(paymentService.verifyCancelForUser(userId, transactionId));
    }
}
