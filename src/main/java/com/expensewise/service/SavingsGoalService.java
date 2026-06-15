package com.expensewise.service;

import com.expensewise.dto.SavingsGoalDTO;
import com.expensewise.dto.SavingsGoalRequest;
import com.expensewise.entity.SavingsGoal;
import com.expensewise.entity.User;
import com.expensewise.exception.BadRequestException;
import com.expensewise.exception.ResourceNotFoundException;
import com.expensewise.repository.SavingsGoalRepository;
import com.expensewise.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public List<SavingsGoalDTO> getGoals(Long userId) {
        return savingsGoalRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SavingsGoalDTO createGoal(Long userId, SavingsGoalRequest request) {
        SavingsGoal goal = SavingsGoal.builder()
                .userId(userId)
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .deadline(request.getDeadline())
                .color(request.getColor())
                .icon(request.getIcon())
                .status("ACTIVE")
                .build();

        goal = savingsGoalRepository.save(goal);
        return toDTO(goal);
    }

    public SavingsGoalDTO updateGoal(Long userId, Long id, SavingsGoalRequest request) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", id));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to update this savings goal");
        }

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDeadline(request.getDeadline());
        goal.setColor(request.getColor());
        goal.setIcon(request.getIcon());

        // Check if current amount now meets or exceeds target
        boolean justCompleted = false;
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            if (!"COMPLETED".equals(goal.getStatus())) {
                justCompleted = true;
            }
            goal.setStatus("COMPLETED");
        } else if ("COMPLETED".equals(goal.getStatus())) {
            // If target was raised above current amount, revert to active
            goal.setStatus("ACTIVE");
        }

        goal = savingsGoalRepository.save(goal);

        if (justCompleted) {
            final String goalName = goal.getName();
            final BigDecimal targetAmount = goal.getTargetAmount();
            userRepository.findById(userId).ifPresent(user ->
                emailService.sendGoalReachedEmail(
                    user.getFullName(), user.getEmail(),
                    goalName, targetAmount)
            );
        }

        return toDTO(goal);
    }

    public SavingsGoalDTO addFunds(Long userId, Long id, BigDecimal amount) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", id));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to add funds to this savings goal");
        }

        if ("CANCELLED".equals(goal.getStatus())) {
            throw new BadRequestException("Cannot add funds to a cancelled savings goal");
        }

        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));

        // Auto-complete if target reached
        boolean justCompleted = false;
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0
                && !"COMPLETED".equals(goal.getStatus())) {
            goal.setStatus("COMPLETED");
            justCompleted = true;
        }

        goal = savingsGoalRepository.save(goal);

        // Send congratulations email
        if (justCompleted) {
            final String goalName = goal.getName();
            final BigDecimal targetAmount = goal.getTargetAmount();
            userRepository.findById(userId).ifPresent(user ->
                emailService.sendGoalReachedEmail(
                    user.getFullName(), user.getEmail(),
                    goalName, targetAmount)
            );
        }

        return toDTO(goal);
    }

    public void deleteGoal(Long userId, Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings Goal", id));

        if (!goal.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to delete this savings goal");
        }

        savingsGoalRepository.delete(goal);
    }

    private SavingsGoalDTO toDTO(SavingsGoal goal) {
        double progressPercentage = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = goal.getCurrentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        // Cap progress at 100%
        if (progressPercentage > 100.0) {
            progressPercentage = 100.0;
        }

        return SavingsGoalDTO.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .deadline(goal.getDeadline())
                .color(goal.getColor())
                .icon(goal.getIcon())
                .status(goal.getStatus())
                .progressPercentage(progressPercentage)
                .createdAt(goal.getCreatedAt())
                .build();
    }
}
