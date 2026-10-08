package com.tripcompanion.expense;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ApiResponse<List<ExpenseDtos.ExpenseView>> list(
            @CurrentUser Long userId,
            @PathVariable Long tripId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(expenseService.list(userId, tripId, category, from, to));
    }

    @GetMapping("/summary")
    public ApiResponse<ExpenseDtos.SummaryView> summary(@CurrentUser Long userId,
                                                        @PathVariable Long tripId) {
        return ApiResponse.ok(expenseService.summary(userId, tripId));
    }

    @PostMapping
    public ApiResponse<ExpenseDtos.ExpenseView> create(@CurrentUser Long userId,
                                                       @PathVariable Long tripId,
                                                       @Valid @RequestBody ExpenseDtos.CreateRequest req) {
        return ApiResponse.ok(expenseService.create(userId, tripId, req));
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseDtos.ExpenseView> update(@CurrentUser Long userId,
                                                       @PathVariable Long tripId,
                                                       @PathVariable Long expenseId,
                                                       @Valid @RequestBody ExpenseDtos.UpdateRequest req) {
        return ApiResponse.ok(expenseService.update(userId, tripId, expenseId, req));
    }

    @DeleteMapping("/{expenseId}")
    public ApiResponse<Void> delete(@CurrentUser Long userId,
                                    @PathVariable Long tripId,
                                    @PathVariable Long expenseId) {
        expenseService.delete(userId, tripId, expenseId);
        return ApiResponse.ok();
    }
}