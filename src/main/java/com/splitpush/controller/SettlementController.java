package com.splitpush.controller;

import com.splitpush.dto.SettlementDTO;
import com.splitpush.model.Settlement;
import com.splitpush.model.User;
import com.splitpush.service.SettlementService;
import com.splitpush.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {
    @Autowired
    private SettlementService settlementService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createSettlement(@Valid @RequestBody SettlementDTO settlementDTO, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Settlement settlement = settlementService.createSettlement(settlementDTO, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(settlement);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getSettlementsByGroup(@PathVariable String groupId, Authentication authentication) {
        try {
            List<Settlement> settlements = settlementService.getSettlementsByGroup(groupId);
            return ResponseEntity.ok(settlements);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> getMySettlements(Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Settlement> settlements = settlementService.getSettlementsByUser(currentUser.getId());
            return ResponseEntity.ok(settlements);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

