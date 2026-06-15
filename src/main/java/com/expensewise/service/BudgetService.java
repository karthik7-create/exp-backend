package com.expensewise.service;

import com.expensewise.dto.BudgetDTO;
import com.expensewise.dto.BudgetRequest;
import com.expensewise.entity.Budget;
import com.expensewise.entity.Category;
import com.expensewise.exception.BadRequestException;
import com.expensewise.exception.ResourceNotFoundException;
import com.expensewise.repository.BudgetRepository;
import com.expensewise.repository.CategoryRepository;
import com.expensewise.repository.TransactionRepository;
import com.expensewise.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         EmailService emailService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public List<BudgetDTO> getBudgets(Long userId, int month, int year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        return budgets.stream()
                .map(budget -> toDTO(budget, userId))
                .collect(Collectors.toList());
    }

    public BudgetDTO createOrUpdateBudget(Long userId, BudgetRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        if (!category.getUserId().equals(userId)) {
            throw new BadRequestException("Category does not belong to the current user");
        }

        Optional<Budget> existingBudget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                userId, request.getCategoryId(), request.getMonth(), request.getYear());

        Budget budget;
        if (existingBudget.isPresent()) {
            budget = existingBudget.get();
            budget.setAmountLimit(request.getAmountLimit());
        } else {
            budget = Budget.builder()
                    .userId(userId)
                    .categoryId(request.getCategoryId())
                    .amountLimit(request.getAmountLimit())
                    .month(request.getMonth())
                    .year(request.getYear())
                    .build();
        }

        Budget saved = budgetRepository.save(budget);

        // Check if the newly set budget limit is already exceeded
        LocalDate startDate = LocalDate.of(saved.getYear(), saved.getMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        BigDecimal spent = transactionRepository.sumExpenseByUserIdAndCategoryAndDateRange(
                userId, saved.getCategoryId(), startDate, endDate);

        if (spent != null && spent.compareTo(saved.getAmountLimit()) >= 0) {
            userRepository.findById(userId).ifPresent(user ->
                emailService.sendBudgetExceededEmail(
                    user.getFullName(), user.getEmail(),
                    category.getName(), saved.getAmountLimit(), spent)
            );
        }

        // Reload to get category relationship
        Budget reloaded = budgetRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", saved.getId()));

        return toDTO(reloaded, userId);
    }

    public void deleteBudget(Long userId, Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));

        if (!budget.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to delete this budget");
        }

        budgetRepository.delete(budget);
    }

    private BudgetDTO toDTO(Budget budget, Long userId) {
        // Calculate spent amount for this category in the budget's month/year
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal spent = transactionRepository.sumExpenseByUserIdAndCategoryAndDateRange(
                userId, budget.getCategoryId(), startDate, endDate);

        BudgetDTO dto = BudgetDTO.builder()
                .id(budget.getId())
                .categoryId(budget.getCategoryId())
                .amountLimit(budget.getAmountLimit())
                .spent(spent != null ? spent : BigDecimal.ZERO)
                .month(budget.getMonth())
                .year(budget.getYear())
                .build();

        if (budget.getCategory() != null) {
            dto.setCategoryName(budget.getCategory().getName());
            dto.setCategoryColor(budget.getCategory().getColor());
            dto.setCategoryIcon(budget.getCategory().getIcon());
        }

        return dto;
    }
}
