package com.turfexplorer.controller;

import com.turfexplorer.dto.MessageResponse;
import com.turfexplorer.dto.SlotRequest;
import com.turfexplorer.dto.SlotResponse;
import com.turfexplorer.security.UserDetailsServiceImpl;
import com.turfexplorer.service.OwnerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timeslots")
public class TimeSlotController {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PutMapping("/{slotId}")
    public ResponseEntity<SlotResponse> updateTimeSlot(
            @PathVariable Long slotId,
            @Valid @RequestBody SlotRequest request,
            Authentication authentication) {
        Long userId = userDetailsService.getUserByEmail(authentication.getName()).getId();
        return ResponseEntity.ok(ownerService.updateSlot(slotId, userId, request));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<MessageResponse> deleteTimeSlot(
            @PathVariable Long slotId,
            Authentication authentication) {
        Long userId = userDetailsService.getUserByEmail(authentication.getName()).getId();
        ownerService.deleteSlot(slotId, userId);
        return ResponseEntity.ok(new MessageResponse("Slot deleted successfully"));
    }
}
