package com.splitpush.controller;

import com.splitpush.dto.BalanceDTO;
import com.splitpush.model.User;
import com.splitpush.service.ExpenseService;
import com.splitpush.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserService userService;

    @GetMapping("/balances")
    public ResponseEntity<List<BalanceDTO>> getBalances(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<BalanceDTO> balances = expenseService.calculateBalances(user.getId());
        return ResponseEntity.ok(balances);
    }
}

