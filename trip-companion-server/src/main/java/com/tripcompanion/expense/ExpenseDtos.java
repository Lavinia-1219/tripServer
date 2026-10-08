package com.tripcompanion.expense;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "不能为空")
            @Size(max = 60, message = "最多 60 个字")
            String title,

            @NotNull(message = "不能为空")
            @Min(value = 1, message = "金额必须大于 0")
            Long amountCents,

            @NotNull(message = "必须指定谁付的钱")
            Long payerId,

            @Size(max = 20)
            String category,

            @NotNull(message = "不能为空")
            LocalDate spentAt,

            @Size(max = 200, message = "最多 200 个字")
            String note
    ) {
    }

    public record UpdateRequest(
            @Size(max = 60, message = "最多 60 个字")
            String title,

            @Min(value = 1, message = "金额必须大于 0")
            Long amountCents,

            Long payerId,

            @Size(max = 20)
            String category,

            LocalDate spentAt,

            @Size(max = 200, message = "最多 200 个字")
            String note
    ) {
    }

    public record ExpenseView(
            Long id,
            Long tripId,
            String title,
            Long amountCents,
            Long payerId,
            String category,
            LocalDate spentAt,
            String note,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CategorySum(
            String category,
            Long amountCents,
            Long count
    ) {
    }

    public record SummaryView(
            Long totalCents,
            Long count,
            Long myPaidCents,
            List<CategorySum> byCategory
    ) {
    }
}