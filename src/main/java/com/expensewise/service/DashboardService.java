package com.expensewise.service;

import com.expensewise.dto.CategoryBreakdown;
import com.expensewise.dto.DashboardSummary;
import com.expensewise.dto.MonthlyTrend;
import com.expensewise.entity.SavingsGoal;
import com.expensewise.entity.Transaction;
import com.expensewise.repository.SavingsGoalRepository;
import com.expensewise.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    public DashboardService(TransactionRepository transactionRepository,
                            SavingsGoalRepository savingsGoalRepository) {
        this.transactionRepository = transactionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    public DashboardSummary getSummary(Long userId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, "INCOME", startDate, endDate);
        BigDecimal totalExpenses = transactionRepository.sumAmountByUserIdAndTypeAndDateRange(
                userId, "EXPENSE", startDate, endDate);

        // Total savings from active and completed savings goals
        List<SavingsGoal> goals = savingsGoalRepository.findByUserId(userId);
        BigDecimal totalSavings = goals.stream()
                .map(SavingsGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalIncome.subtract(totalExpenses);

        long transactionCount = transactionRepository.countByUserIdAndDateRange(userId, startDate, endDate);

        return DashboardSummary.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .totalSavings(totalSavings)
                .balance(balance)
                .transactionCount(transactionCount)
                .build();
    }

    public List<CategoryBreakdown> getCategoryBreakdown(Long userId, int month, int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Object[]> results = transactionRepository.findCategoryBreakdown(userId, startDate, endDate);

        // Calculate total for percentage
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : results) {
            BigDecimal amount = (BigDecimal) row[2];
            total = total.add(amount);
        }

        List<CategoryBreakdown> breakdowns = new ArrayList<>();
        for (Object[] row : results) {
            String categoryName = (String) row[0];
            String categoryColor = (String) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            double percentage = 0.0;
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(total, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            breakdowns.add(CategoryBreakdown.builder()
                    .categoryName(categoryName)
                    .categoryColor(categoryColor)
                    .amount(amount)
                    .percentage(percentage)
                    .build());
        }

        return breakdowns;
    }

    public List<MonthlyTrend> getMonthlyTrend(Long userId, int year) {
        List<Object[]> results = transactionRepository.findMonthlyTrend(userId, year);

        List<MonthlyTrend> trends = new ArrayList<>();
        for (Object[] row : results) {
            int monthNum = ((Number) row[0]).intValue();
            BigDecimal expense = (BigDecimal) row[1];
            BigDecimal income = (BigDecimal) row[2];

            String monthName = Month.of(monthNum).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            trends.add(MonthlyTrend.builder()
                    .month(monthNum)
                    .monthName(monthName)
                    .income(income)
                    .expense(expense)
                    .build());
        }

        return trends;
    }

    public byte[] exportTransactionsCsv(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        java.util.Map<String, BigDecimal> expenseBreakdown = new java.util.HashMap<>();
        java.util.Map<String, BigDecimal> incomeBreakdown = new java.util.HashMap<>();

        for (Transaction t : transactions) {
            String catName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
            if ("INCOME".equalsIgnoreCase(t.getType())) {
                totalIncome = totalIncome.add(t.getAmount());
                incomeBreakdown.put(catName, incomeBreakdown.getOrDefault(catName, BigDecimal.ZERO).add(t.getAmount()));
            } else {
                totalExpense = totalExpense.add(t.getAmount());
                expenseBreakdown.put(catName, expenseBreakdown.getOrDefault(catName, BigDecimal.ZERO).add(t.getAmount()));
            }
        }

        StringBuilder csv = new StringBuilder();
        
        csv.append("FINANCIAL SUMMARY\n");
        csv.append("Period:,").append(startDate).append(" to ").append(endDate).append("\n");
        csv.append("Total Income:,").append(totalIncome).append("\n");
        csv.append("Total Expenses:,").append(totalExpense).append("\n");
        csv.append("Net Balance:,").append(totalIncome.subtract(totalExpense)).append("\n");
        csv.append("\n");

        csv.append("WHERE YOU SPEND THE MOST (EXPENSES)\n");
        csv.append("Category,Total Spent\n");
        expenseBreakdown.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(e -> csv.append(escapeCsv(e.getKey())).append(",").append(e.getValue()).append("\n"));
        csv.append("\n");

        csv.append("INCOME SOURCES\n");
        csv.append("Category,Total Income\n");
        incomeBreakdown.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(e -> csv.append(escapeCsv(e.getKey())).append(",").append(e.getValue()).append("\n"));
        csv.append("\n");

        csv.append("DETAILED TRANSACTIONS\n");
        csv.append("Date,Type,Category,Description,Amount,Notes\n");

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy");

        for (Transaction t : transactions) {
            String categoryName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
            String formattedDate = t.getTransactionDate() != null ? t.getTransactionDate().format(formatter) : "";

            csv.append(formattedDate).append(",");
            csv.append(t.getType()).append(",");
            csv.append(escapeCsv(categoryName)).append(",");
            csv.append(escapeCsv(t.getDescription())).append(",");
            csv.append(t.getAmount()).append(",");
            csv.append(escapeCsv(t.getNotes() != null ? t.getNotes() : "")).append("\n");
        }

        return csv.toString().getBytes();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
