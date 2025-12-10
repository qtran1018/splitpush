package com.splitpush.controller;

import com.splitpush.dto.ExpenseDTO;
import com.splitpush.model.Expense;
import com.splitpush.model.User;
import com.splitpush.service.ExpenseService;
import com.splitpush.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createExpense(@Valid @RequestBody ExpenseDTO expenseDTO, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check access in service layer
            Expense expense = expenseService.createExpense(expenseDTO, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(expense);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getExpensesByGroup(
            @PathVariable String groupId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("[CACHE DEBUG] Controller: Request received for groupId=" + groupId + ", userId=" + currentUser.getId() + ", page=" + page + ", size=" + size);
            Page<Expense> expensePage = expenseService.getExpensesByTripGroup(groupId, currentUser.getId(), page, size);
            System.out.println("[CACHE DEBUG] Controller: Service returned " + expensePage.getContent().size() + " expenses");
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", expensePage.getContent());
            response.put("totalElements", expensePage.getTotalElements());
            response.put("totalPages", expensePage.getTotalPages());
            response.put("currentPage", expensePage.getNumber());
            response.put("pageSize", expensePage.getSize());
            response.put("hasNext", expensePage.hasNext());
            response.put("hasPrevious", expensePage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExpense(@PathVariable Long id) {
        return expenseService.getExpenseById(id)
                .map(expense -> ResponseEntity.ok(expense))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{expenseId}/participants/{userId}/pay")
    public ResponseEntity<?> markAsPaid(@PathVariable Long expenseId, @PathVariable Long userId) {
        try {
            expenseService.markExpenseParticipantAsPaid(expenseId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Expense marked as paid");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseDTO expenseDTO, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Expense expense = expenseService.updateExpense(id, expenseDTO, currentUser.getId());
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Expense expense = expenseService.getExpenseById(id)
                    .orElseThrow(() -> new RuntimeException("Expense not found"));

            // Check if user is a member of the group - compare by ID
            boolean isMember = expense.getTripGroup().getMembers().stream()
                    .anyMatch(member -> member.getId().equals(currentUser.getId()));
            if (!isMember) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You must be a member of this group to delete expenses");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            expenseService.deleteExpense(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Expense deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

