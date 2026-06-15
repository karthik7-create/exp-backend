package com.expensewise.controller;

import com.expensewise.dto.CategoryBreakdown;
import com.expensewise.dto.DashboardSummary;
import com.expensewise.dto.MonthlyTrend;
import com.expensewise.entity.User;
import com.expensewise.service.AuthService;
import com.expensewise.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;

    public DashboardController(DashboardService dashboardService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = authService.getCurrentUser(auth.getName());
        return user.getId();
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> getSummary(@RequestParam int month, @RequestParam int year) {
        Long userId = getCurrentUserId();
        DashboardSummary summary = dashboardService.getSummary(userId, month, year);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/dashboard/category-breakdown")
    public ResponseEntity<List<CategoryBreakdown>> getCategoryBreakdown(@RequestParam int month,
                                                                         @RequestParam int year) {
        Long userId = getCurrentUserId();
        List<CategoryBreakdown> breakdown = dashboardService.getCategoryBreakdown(userId, month, year);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/dashboard/monthly-trend")
    public ResponseEntity<List<MonthlyTrend>> getMonthlyTrend(@RequestParam int year) {
        Long userId = getCurrentUserId();
        List<MonthlyTrend> trend = dashboardService.getMonthlyTrend(userId, year);
        return ResponseEntity.ok(trend);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportTransactionsCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long userId = getCurrentUserId();
        byte[] csvData = dashboardService.exportTransactionsCsv(userId, startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "transactions_export.csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }
}
