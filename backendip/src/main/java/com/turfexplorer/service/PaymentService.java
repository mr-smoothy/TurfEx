package com.turfexplorer.service;

import com.turfexplorer.entity.Booking;
import com.turfexplorer.entity.Slot;
import com.turfexplorer.entity.Transaction;
import com.turfexplorer.entity.User;
import com.turfexplorer.enums.BookingStatus;
import com.turfexplorer.enums.TransactionStatus;
import com.turfexplorer.exception.BadRequestException;
import com.turfexplorer.exception.ResourceNotFoundException;
import com.turfexplorer.repository.BookingRepository;
import com.turfexplorer.repository.SlotRepository;
import com.turfexplorer.repository.TransactionRepository;
import com.turfexplorer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${bkash.app.key:}")
    private String bkashAppKey;

    @Value("${bkash.app.secret:}")
    private String bkashAppSecret;

    @Value("${bkash.username:}")
    private String bkashUsername;

    @Value("${bkash.password:}")
    private String bkashPassword;

    @Value("${bkash.base.url:}")
    private String bkashBaseUrl;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${bkash.default.payer.reference:01770618575}")
    private String defaultPayerReference;

    private volatile boolean transactionSchemaChecked = false;

    @Transactional
    public Map<String, Object> createBkashPayment(Long userId, Long bookingId) {
        validateBkashConfig();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new BadRequestException("You can only pay for your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cancelled bookings cannot be paid");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking is already confirmed");
        }

        Slot slot = slotRepository.findById(booking.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (slot.getPrice() == null) {
            throw new BadRequestException("Slot price is missing");
        }

        double amount = slot.getPrice();
        String amountAsString = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String payerReference = buildPayerReference(user.getPhone());

        String token = grantToken();
        Map<String, Object> createResponse = createPayment(token, booking.getId(), amountAsString, payerReference);
        log.info("bKash create payment raw response for bookingId={}: {}", booking.getId(), createResponse);

        String statusCode = getFirstNonBlank(createResponse, "statusCode", "status_code");
        String statusMessage = getFirstNonBlank(createResponse, "statusMessage", "status_message", "message");
        if (StringUtils.hasText(statusCode) && !"0000".equals(statusCode)) {
            throw new BadRequestException("bKash create payment failed [" + statusCode + "]: "
                    + (StringUtils.hasText(statusMessage) ? statusMessage : "Unknown error"));
        }

        String paymentId = getFirstNonBlank(createResponse, "paymentID", "paymentId", "paymentid");
        String bkashUrl = getFirstNonBlank(createResponse, "bkashURL", "bkashUrl", "bkashurl");

        if (!StringUtils.hasText(paymentId)) {
            throw new BadRequestException("bKash create payment did not return paymentID. Response: " + createResponse);
        }

        if (!StringUtils.hasText(bkashUrl)) {
            throw new BadRequestException("bKash create payment did not return bkashURL. Response: " + createResponse);
        }

        Transaction transaction = new Transaction();
        transaction.setBookingId(booking.getId());
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setPaymentId(paymentId);
        try {
            ensureTransactionSchemaCompatible();
            Transaction existing = transactionRepository.findByPaymentId(paymentId).orElse(null);
            if (existing != null) {
                existing.setBookingId(booking.getId());
                existing.setAmount(amount);
                existing.setStatus(TransactionStatus.PENDING);
                transactionRepository.save(existing);
            } else {
                transactionRepository.save(transaction);
            }
        } catch (Exception ex) {
            log.error("Failed to save transaction for bookingId={} paymentId={}", booking.getId(), paymentId, ex);
            String cause = ex.getMessage();
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                cause = ex.getCause().getMessage();
            }
            throw new BadRequestException("Failed to save payment transaction: " + cause);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("bkashURL", bkashUrl);
        response.put("paymentID", paymentId);
        return response;
    }

    @Transactional
    public Map<String, Object> executeBkashPayment(Long userId, String paymentId) {
        validateBkashConfig();

        if (!StringUtils.hasText(paymentId)) {
            throw new BadRequestException("paymentID is required");
        }

        Transaction transaction = transactionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for this paymentID"));

        Booking booking = bookingRepository.findById(transaction.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for transaction"));

        if (!booking.getUserId().equals(userId)) {
            throw new BadRequestException("You can only execute payment for your own bookings");
        }

        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            return buildExecuteResponse(transaction, booking, paymentId, "ALREADY_EXECUTED");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new BadRequestException("Booking is cancelled. Payment cannot be executed");
        }

        String token = grantToken();
        Map<String, Object> executeResponse = executePayment(token, paymentId);

        String transactionStatus = asString(executeResponse.get("transactionStatus"));
        String statusCode = asString(executeResponse.get("statusCode"));

        if ("Completed".equalsIgnoreCase(transactionStatus) || "0000".equals(statusCode)) {
            markPaymentSuccess(transaction, booking);
            return buildExecuteResponse(transaction, booking, paymentId, "SUCCESS");
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);

        String statusMessage = asString(executeResponse.get("statusMessage"));
        throw new BadRequestException("bKash execute failed: " + (StringUtils.hasText(statusMessage) ? statusMessage : "Payment not completed"));
    }

    private Map<String, Object> buildExecuteResponse(Transaction transaction, Booking booking, String paymentId, String result) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("paymentID", paymentId);
        response.put("amount", transaction.getAmount());
        response.put("transactionStatus", transaction.getStatus().name());
        response.put("bookingStatus", booking.getStatus().name());
        return response;
    }

    private void markPaymentSuccess(Transaction transaction, Booking booking) {
        boolean slotAlreadyConfirmedByAnotherBooking = bookingRepository
                .findBySlotIdAndBookingDateAndStatus(booking.getSlotId(), booking.getBookingDate(), BookingStatus.CONFIRMED)
                .filter(existing -> !existing.getId().equals(booking.getId()))
                .isPresent();

        if (slotAlreadyConfirmedByAnotherBooking) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new BadRequestException("Slot already confirmed by another booking");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
    }

    private String grantToken() {
        String url = normalizeBaseUrl() + "/checkout/token/grant";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", bkashUsername);
        headers.set("password", bkashPassword);

        Map<String, String> body = new HashMap<>();
        body.put("app_key", bkashAppKey);
        body.put("app_secret", bkashAppSecret);

        Map<String, Object> payload = postForMap(url, headers, body, "grant token");
        String idToken = asString(payload.get("id_token"));

        if (!StringUtils.hasText(idToken)) {
            throw new BadRequestException("bKash token grant response missing id_token");
        }

        return idToken;
    }

    private Map<String, Object> createPayment(String idToken, Long bookingId, String amountAsString, String payerReference) {
        String url = normalizeBaseUrl() + "/checkout/create";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", idToken);
        headers.set("X-APP-Key", bkashAppKey);

        Map<String, String> body = new HashMap<>();
        body.put("mode", "0011");
        body.put("amount", amountAsString);
        body.put("currency", "BDT");
        body.put("intent", "sale");
        body.put("payerReference", payerReference);
        body.put("merchantInvoiceNumber", String.valueOf(bookingId));
        body.put("callbackURL", "http://localhost:3000/payment-success");

        return postForMap(url, headers, body, "create payment");
    }

    private Map<String, Object> executePayment(String idToken, String paymentId) {
        String url = normalizeBaseUrl() + "/checkout/execute";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", idToken);
        headers.set("X-APP-Key", bkashAppKey);

        Map<String, String> body = new HashMap<>();
        body.put("paymentID", paymentId);

        return postForMap(url, headers, body, "execute payment");
    }

    private Map<String, Object> postForMap(String url, HttpHeaders headers, Map<String, String> body, String operation) {
        try {
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() == null) {
                throw new BadRequestException("bKash " + operation + " returned empty response");
            }

            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.error("bKash {} failed. status={} body={}", operation, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadRequestException("bKash " + operation + " failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("bKash {} failed: {}", operation, ex.getMessage(), ex);
            throw new BadRequestException("bKash " + operation + " failed: " + ex.getMessage());
        }
    }

    private String normalizeBaseUrl() {
        String url = bkashBaseUrl;
        if (!StringUtils.hasText(url)) {
            throw new BadRequestException("bKash base url is not configured");
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private void validateBkashConfig() {
        if (!StringUtils.hasText(bkashAppKey)) {
            throw new BadRequestException("bKash app key is not configured");
        }
        if (!StringUtils.hasText(bkashAppSecret)) {
            throw new BadRequestException("bKash app secret is not configured");
        }
        if (!StringUtils.hasText(bkashUsername)) {
            throw new BadRequestException("bKash username is not configured");
        }
        if (!StringUtils.hasText(bkashPassword)) {
            throw new BadRequestException("bKash password is not configured");
        }
        if (!StringUtils.hasText(bkashBaseUrl)) {
            throw new BadRequestException("bKash base url is not configured");
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private String buildPayerReference(String phone) {
        if (StringUtils.hasText(phone)) {
            String digits = phone.replaceAll("\\D", "");
            if (digits.startsWith("880") && digits.length() == 13) {
                digits = "0" + digits.substring(3);
            } else if (digits.startsWith("88") && digits.length() == 13) {
                digits = "0" + digits.substring(2);
            }

            if (digits.matches("01\\d{9}")) {
                return digits;
            }
        }

        return defaultPayerReference;
    }

    private synchronized void ensureTransactionSchemaCompatible() {
        if (transactionSchemaChecked) {
            return;
        }

        Integer stripeColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'transactions' AND column_name = 'stripe_session_id'",
                Integer.class
        );

        Integer paymentColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'transactions' AND column_name = 'payment_id'",
                Integer.class
        );

        boolean hasStripeColumn = stripeColumnCount != null && stripeColumnCount > 0;
        boolean hasPaymentColumn = paymentColumnCount != null && paymentColumnCount > 0;

        if (!hasStripeColumn && hasPaymentColumn) {
            log.warn("transactions.stripe_session_id missing; applying compatibility migration from payment_id");
            executeDdlSafely("ALTER TABLE transactions ADD COLUMN stripe_session_id VARCHAR(255) NULL");
            executeDdlSafely("UPDATE transactions SET stripe_session_id = payment_id WHERE stripe_session_id IS NULL");
            executeDdlSafely("ALTER TABLE transactions MODIFY stripe_session_id VARCHAR(255) NOT NULL");
            executeDdlSafely("CREATE UNIQUE INDEX ux_transactions_stripe_session_id ON transactions(stripe_session_id)");
        }

        if (hasStripeColumn && !hasPaymentColumn) {
            log.warn("transactions.payment_id missing; applying compatibility migration from stripe_session_id");
            executeDdlSafely("ALTER TABLE transactions ADD COLUMN payment_id VARCHAR(255) NULL");
            executeDdlSafely("UPDATE transactions SET payment_id = stripe_session_id WHERE payment_id IS NULL");
            executeDdlSafely("ALTER TABLE transactions MODIFY payment_id VARCHAR(255) NOT NULL");
            executeDdlSafely("CREATE UNIQUE INDEX ux_transactions_payment_id ON transactions(payment_id)");
        }

        transactionSchemaChecked = true;
    }

    private void executeDdlSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            log.debug("Ignoring schema compatibility SQL failure for [{}]: {}", sql, ex.getMessage());
        }
    }

    private String getFirstNonBlank(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = asString(payload.get(key));
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
