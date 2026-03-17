package com.turfexplorer.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final String API_KEY = "AIzaSyATpDR78TFOJYXnnB6I2zrwGam_WoUxg6Y";

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        
        // Fast-path for quick keywords
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("book")) {
            return Map.of("reply", "To book a turf, open a turf from the Home page and select an available slot.");
        } else if (lowerMessage.contains("cancel")) {
            return Map.of("reply", "You can cancel bookings easily from your My Bookings dashboard.");
        } else if (lowerMessage.contains("login") || lowerMessage.contains("sign up")) {
            return Map.of("reply", "You can login or register using the links in the top navigation bar.");
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + API_KEY;
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", message)));
            body.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // Safe JSON parse logic for Google Gemini AI
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> respBody = response.getBody();
                
                if (respBody.containsKey("candidates")) {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) respBody.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> candidateContent = (Map<String, Object>) candidates.get(0).get("content");
                        if (candidateContent != null && candidateContent.containsKey("parts")) {
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                String replyText = (String) parts.get(0).get("text");
                                return Map.of("reply", replyText);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Failsafe exception handler so backend wont crash
            e.printStackTrace();
        }

        // Graceful fallback incase the LLM API fails or rate-limits
        return Map.of("reply", "I am having trouble connecting to my AI brain at the moment. However, I can still help you book, cancel, or find a turf!");
    }
}

