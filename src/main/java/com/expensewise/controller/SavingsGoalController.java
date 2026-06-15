package com.expensewise.controller;

import com.expensewise.dto.AddFundsRequest;
import com.expensewise.dto.SavingsGoalDTO;
import com.expensewise.dto.SavingsGoalRequest;
import com.expensewise.entity.User;
import com.expensewise.service.AuthService;
import com.expensewise.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;
    private final AuthService authService;

    public SavingsGoalController(SavingsGoalService savingsGoalService, AuthService authService) {
        this.savingsGoalService = savingsGoalService;
        this.authService = authService;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = authService.getCurrentUser(auth.getName());
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoalDTO>> getGoals() {
        Long userId = getCurrentUserId();
        List<SavingsGoalDTO> goals = savingsGoalService.getGoals(userId);
        return ResponseEntity.ok(goals);
    }

    @PostMapping
    public ResponseEntity<SavingsGoalDTO> createGoal(@Valid @RequestBody SavingsGoalRequest request) {
        Long userId = getCurrentUserId();
        SavingsGoalDTO goal = savingsGoalService.createGoal(userId, request);
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalDTO> updateGoal(@PathVariable Long id,
                                                      @Valid @RequestBody SavingsGoalRequest request) {
        Long userId = getCurrentUserId();
        SavingsGoalDTO goal = savingsGoalService.updateGoal(userId, id, request);
        return ResponseEntity.ok(goal);
    }

    @PostMapping("/{id}/add-funds")
    public ResponseEntity<SavingsGoalDTO> addFunds(@PathVariable Long id,
                                                    @Valid @RequestBody AddFundsRequest request) {
        Long userId = getCurrentUserId();
        SavingsGoalDTO goal = savingsGoalService.addFunds(userId, id, request.getAmount());
        return ResponseEntity.ok(goal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        savingsGoalService.deleteGoal(userId, id);
        return ResponseEntity.noContent().build();
    }
}
