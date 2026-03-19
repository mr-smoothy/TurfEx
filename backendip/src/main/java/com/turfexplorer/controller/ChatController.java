package com.turfexplorer.controller;

import com.turfexplorer.enums.BookingStatus;
import com.turfexplorer.enums.PaymentStatus;
import com.turfexplorer.enums.Role;
import com.turfexplorer.enums.TurfStatus;
import com.turfexplorer.repository.BookingRepository;
import com.turfexplorer.repository.PaymentRepository;
import com.turfexplorer.repository.TurfRepository;
import com.turfexplorer.repository.UserRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TurfRepository turfRepository;
    private final UserRepository userRepository;

    private final Map<String, String> lastIntentBySession = new ConcurrentHashMap<>();

    public ChatController(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            TurfRepository turfRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.turfRepository = turfRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "").trim();
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        String userRole = request.getOrDefault("userRole", "guest").trim().toLowerCase(Locale.ROOT);
        String userName = request.getOrDefault("userName", "friend").trim();

        if (message.isBlank()) {
            return Map.of(
                    "reply",
                    "Hi " + safeName(userName) + "! I can help with booking, payment, owner/admin actions, and live platform stats."
            );
        }

        String q = message.toLowerCase(Locale.ROOT);
        String intent = detectIntent(q, sessionId);
        lastIntentBySession.put(sessionId, intent);

        if ("stats".equals(intent)) {
            long totalTurfs = turfRepository.count();
            long approvedTurfs = turfRepository.countByStatus(TurfStatus.APPROVED);
            long pendingTurfs = turfRepository.countByStatus(TurfStatus.PENDING);
            long totalBookings = bookingRepository.count();
            long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
            long pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING);
                long successPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS)
                    + paymentRepository.countByStatus(PaymentStatus.PARTIAL)
                    + paymentRepository.countByStatus(PaymentStatus.FULL);

            String statsReply = "Live snapshot: turfs " + approvedTurfs + "/" + totalTurfs
                    + " approved, " + pendingTurfs + " pending approval; bookings " + confirmedBookings
                    + "/" + totalBookings + " confirmed; payments " + successPayments
                    + " success, " + pendingPayments + " pending.";

            return Map.of("reply", statsReply);
        }

        if ("booking".equals(intent)) {
            long totalBookings = bookingRepository.count();
            long confirmed = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
            return Map.of(
                    "reply",
                    "To book: open Turfs, pick turf/date/slot, then Pay Now. Booking confirms only after payment verification."
                            + " Current confirmation trend: " + confirmed + " of " + totalBookings + " bookings are confirmed."
            );
        }

        if ("payment".equals(intent)) {
            long success = paymentRepository.countByStatus(PaymentStatus.SUCCESS)
                    + paymentRepository.countByStatus(PaymentStatus.PARTIAL)
                    + paymentRepository.countByStatus(PaymentStatus.FULL);
            long failed = paymentRepository.countByStatus(PaymentStatus.FAILED);
            long cancelled = paymentRepository.countByStatus(PaymentStatus.CANCELLED);
            long pending = paymentRepository.countByStatus(PaymentStatus.PENDING);
            return Map.of(
                    "reply",
                    "Payment flow: Pay Now -> gateway redirect -> callback/verify -> booking confirmation only on success."
                            + " Live payment status totals: success " + success
                            + ", failed " + failed
                            + ", cancelled " + cancelled
                            + ", pending " + pending + "."
            );
        }

        if ("cancel".equals(intent)) {
            return Map.of("reply", "You can cancel from My Bookings. Open My Bookings, find your booking, and click Cancel.");
        }

        if ("login".equals(intent)) {
            return Map.of("reply", "Click Login in the header, then enter your registered email and password.");
        }

        if ("register".equals(intent)) {
            return Map.of("reply", "Click Register/Sign Up from the header and create your Turf Explorer account.");
        }

        if ("owner".equals(intent)) {
            long totalOwners = userRepository.countByRole(Role.OWNER);
            long pendingTurfs = turfRepository.countByStatus(TurfStatus.PENDING);
            return Map.of(
                    "reply",
                    "Owner flow: Add Turf -> wait for admin approval -> manage slots/prices in My Turfs."
                            + " There are currently " + totalOwners + " owner accounts and " + pendingTurfs
                            + " turfs waiting for approval."
            );
        }

        if ("admin".equals(intent)) {
            long pendingTurfs = turfRepository.countByStatus(TurfStatus.PENDING);
            long approvedTurfs = turfRepository.countByStatus(TurfStatus.APPROVED);
            return Map.of(
                    "reply",
                    "Admin flow: review turfs in Admin Dashboard and approve/reject."
                            + " Queue now: " + pendingTurfs + " pending, " + approvedTurfs + " approved total."
            );
        }

        if ("pricing".equals(intent)) {
            return Map.of("reply", "Pricing is defined by slot. Final payable amount comes from the selected slot price.");
        }

        if ("status".equals(intent)) {
            long confirmed = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
            long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
            long cancelled = bookingRepository.countByStatus(BookingStatus.CANCELLED);
            return Map.of(
                    "reply",
                    "Booking status updates after payment verification."
                            + " Current totals: confirmed " + confirmed
                            + ", pending " + pending
                            + ", cancelled " + cancelled + "."
            );
        }

        if ("support".equals(intent)) {
            String roleHint = switch (userRole) {
                case "admin" -> "As admin, you can cross-check payment and booking records from dashboard APIs.";
                case "owner" -> "As owner, verify your turf/slot settings and share booking id with support.";
                default -> "Share booking id and transaction id with support for fastest resolution.";
            };
            return Map.of("reply", "For support, share your booking id and transaction id with admin/project owner for quick troubleshooting.");
        }

        if ("greeting".equals(intent)) {
            return Map.of("reply", "Hello " + safeName(userName) + "! Ask me anything about booking, payments, admin/owner flow, or type 'stats' for live platform numbers.");
        }

        return Map.of(
                "reply",
                "I can help with booking, payment, cancel, login/register, owner/admin tasks, pricing, and live stats."
                        + " Try asking: 'show stats' or 'why booking pending?'."
        );
    }

    private String detectIntent(String q, String sessionId) {
        String lastIntent = lastIntentBySession.getOrDefault(sessionId, "general");

        if (containsAny(q, "hi", "hello", "hey")) return "greeting";
        if (containsAny(q, "stats", "summary", "overall", "dashboard")) return "stats";
        if (containsAny(q, "book", "booking", "reserve", "slot")) return "booking";
        if (containsAny(q, "payment", "pay", "gateway", "transaction", "success", "failed")) return "payment";
        if (containsAny(q, "cancel", "refund")) return "cancel";
        if (containsAny(q, "login", "sign in")) return "login";
        if (containsAny(q, "register", "signup", "sign up", "create account")) return "register";
        if (containsAny(q, "owner", "add turf", "my turfs", "slot management")) return "owner";
        if (containsAny(q, "admin", "approve", "approval", "reject")) return "admin";
        if (containsAny(q, "price", "pricing", "cost", "amount")) return "pricing";
        if (containsAny(q, "status", "confirmed", "not confirmed", "pending")) return "status";
        if (containsAny(q, "support", "help", "contact")) return "support";

        if (containsAny(q, "that", "this", "it", "what about", "and then")) {
            return lastIntent;
        }

        return "general";
    }

    private boolean containsAny(String source, String... terms) {
        for (String term : terms) {
            if (source.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String safeName(String userName) {
        return userName == null || userName.isBlank() ? "there" : userName;
    }
}
