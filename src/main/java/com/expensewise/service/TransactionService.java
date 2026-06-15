package com.expensewise.service;

import com.expensewise.dto.TransactionDTO;
import com.expensewise.dto.TransactionRequest;
import com.expensewise.entity.Budget;
import com.expensewise.entity.Category;
import com.expensewise.entity.Transaction;
import com.expensewise.exception.BadRequestException;
import com.expensewise.exception.ResourceNotFoundException;
import com.expensewise.repository.BudgetRepository;
import com.expensewise.repository.CategoryRepository;
import com.expensewise.repository.TransactionRepository;
import com.expensewise.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.time.LocalDate;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              BudgetRepository budgetRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public Page<TransactionDTO> getTransactions(Long userId, String type, Long categoryId,
                                                 LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByUserIdWithFilters(
                userId, type, categoryId, startDate, endDate, pageable);
        return transactions.map(this::toDTO);
    }

    public TransactionDTO createTransaction(Long userId, TransactionRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        if (!category.getUserId().equals(userId)) {
            throw new BadRequestException("Category does not belong to the current user");
        }

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .categoryId(request.getCategoryId())
                .type(request.getType().toUpperCase())
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // Check budget exceeded for EXPENSE transactions
        if ("EXPENSE".equals(saved.getType())) {
            checkBudgetExceeded(userId, saved.getCategoryId(), saved.getTransactionDate());
        }

        // Reload to get category relationship
        Transaction reloaded = transactionRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", saved.getId()));

        return toDTO(reloaded);
    }

    public TransactionDTO updateTransaction(Long userId, Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to update this transaction");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        if (!category.getUserId().equals(userId)) {
            throw new BadRequestException("Category does not belong to the current user");
        }

        transaction.setCategoryId(request.getCategoryId());
        transaction.setType(request.getType().toUpperCase());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNotes(request.getNotes());

        Transaction saved = transactionRepository.save(transaction);

        // Reload to get updated category relationship
        Transaction reloaded = transactionRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", saved.getId()));

        return toDTO(reloaded);
    }

    public void deleteTransaction(Long userId, Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to delete this transaction");
        }

        transactionRepository.delete(transaction);
    }

    private TransactionDTO toDTO(Transaction transaction) {
        TransactionDTO dto = TransactionDTO.builder()
                .id(transaction.getId())
                .categoryId(transaction.getCategoryId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .notes(transaction.getNotes())
                .createdAt(transaction.getCreatedAt())
                .build();

        if (transaction.getCategory() != null) {
            dto.setCategoryName(transaction.getCategory().getName());
            dto.setCategoryColor(transaction.getCategory().getColor());
            dto.setCategoryIcon(transaction.getCategory().getIcon());
        }

        return dto;
    }

    private void checkBudgetExceeded(Long userId, Long categoryId, java.time.LocalDate transactionDate) {
        int month = transactionDate.getMonthValue();
        int year = transactionDate.getYear();

        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year)
            .ifPresent(budget -> {
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                BigDecimal spent = transactionRepository.sumExpenseByUserIdAndCategoryAndDateRange(
                        userId, categoryId, startDate, endDate);

                if (spent.compareTo(budget.getAmountLimit()) >= 0) {
                    Category cat = categoryRepository.findById(categoryId).orElse(null);
                    String categoryName = cat != null ? cat.getName() : "Unknown";

                    userRepository.findById(userId).ifPresent(user ->
                        emailService.sendBudgetExceededEmail(
                            user.getFullName(), user.getEmail(),
                            categoryName, budget.getAmountLimit(), spent)
                    );
                }
            });
    }
}
