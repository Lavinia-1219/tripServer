package com.tripcompanion.checklist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ChecklistDtos {

    private ChecklistDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "不能为空")
            @Size(max = 50, message = "最多 50 个字")
            String name,

            @Size(max = 20)
            String category,

            @Min(value = 1, message = "至少 1 个")
            Integer quantity,

            Long assigneeId
    ) {
    }

    public record UpdateRequest(
            @Size(max = 50, message = "最多 50 个字")
            String name,

            @Size(max = 20)
            String category,

            @Min(value = 1, message = "至少 1 个")
            Integer quantity,

            Long assigneeId
    ) {
    }

    public record StatusRequest(
            @NotBlank(message = "不能为空")
            String status
    ) {
    }

    public record ItemView(
            Long id,
            Long tripId,
            String name,
            String category,
            Integer quantity,
            Long assigneeId,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record Suggestion(
            String name,
            String category,
            String reason,
            List<LocalDate> dates
    ) {
    }

    public record SuggestionView(
            Long tripId,
            String city,
            boolean weatherAvailable,
            String message,
            int days,
            List<Suggestion> suggestions
    ) {
    }

    public record ApplyResult(
            int requestedCount,
            int addedCount,
            List<ItemView> added
    ) {
    }

    public record RuleRequest(
            @NotBlank(message = "不能为空")
            @Size(max = 30, message = "最多 30 个字符")
            String field,

            @NotBlank(message = "不能为空")
            @Size(max = 10, message = "最多 10 个字符")
            String operator,

            @NotNull(message = "不能为空")
            Double threshold,

            @NotBlank(message = "不能为空")
            @Size(max = 50, message = "最多 50 个字")
            String itemName,

            @Size(max = 20, message = "最多 20 个字")
            String category,

            @Size(max = 120, message = "最多 120 个字")
            String reason,

            Integer priority
    ) {
    }

    public record RuleView(
            Long id,
            String field,
            String operator,
            Double threshold,
            String itemName,
            String category,
            String reason,
            Integer priority,
            Boolean enabled,
            String expression
    ) {
    }
}