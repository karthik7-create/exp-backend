package com.expensewise.controller;

import com.expensewise.dto.BudgetDTO;
import com.expensewise.dto.BudgetRequest;
import com.expensewise.entity.User;
import com.expensewise.service.AuthService;
import com.expensewise.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final AuthService authService;

    public BudgetController(BudgetService budgetService, AuthService authService) {
        this.budgetService = budgetService;
        this.authService = authService;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = authService.getCurrentUser(auth.getName());
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getBudgets(@RequestParam int month, @RequestParam int year) {
        Long userId = getCurrentUserId();
        List<BudgetDTO> budgets = budgetService.getBudgets(userId, month, year);
        return ResponseEntity.ok(budgets);
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createOrUpdateBudget(@Valid @RequestBody BudgetRequest request) {
        Long userId = getCurrentUserId();
        BudgetDTO budget = budgetService.createOrUpdateBudget(userId, request);
        return ResponseEntity.ok(budget);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }
}
