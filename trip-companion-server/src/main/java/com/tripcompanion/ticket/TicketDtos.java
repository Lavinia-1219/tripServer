package com.tripcompanion.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class TicketDtos {

    private TicketDtos() {
    }

    public record CreateRequest(

            String type,

            @Size(max = 40, message = "最多 40 个字符")
            String code,

            @NotBlank(message = "不能为空")
            @Size(max = 60, message = "最多 60 个字")
            String fromStation,

            @NotBlank(message = "不能为空")
            @Size(max = 60, message = "最多 60 个字")
            String toStation,

            @NotNull(message = "出发时间不能为空")
            LocalDateTime departAt,

            LocalDateTime arriveAt,

            @Size(max = 40, message = "最多 40 个字")
            String seat,

            @Size(max = 200, message = "最多 200 个字")
            String note
    ) {
    }

    public record UpdateRequest(
            String type,
            @Size(max = 40) String code,
            @Size(max = 60) String fromStation,
            @Size(max = 60) String toStation,
            LocalDateTime departAt,
            LocalDateTime arriveAt,
            @Size(max = 40) String seat,
            @Size(max = 200) String note
    ) {
    }

    public record TicketView(
            Long id,
            Long tripId,
            Long ownerId,
            String type,
            String typeLabel,
            String code,
            String fromStation,
            String toStation,
            LocalDateTime departAt,
            LocalDateTime arriveAt,
            String seat,
            String note,
            LocalDateTime reminderSentAt,
            boolean reminded,
            String countdown,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record TripTicketsView(
            Long tripId,
            List<TicketView> tickets,
            List<com.tripcompanion.reminder.ReminderDtos.ReminderView> reminders
    ) {
    }
}
