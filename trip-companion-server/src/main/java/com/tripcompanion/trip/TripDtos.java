package com.tripcompanion.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TripDtos {

    private TripDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "不能为空")
            @Size(max = 60, message = "最多 60 个字")
            String title,

            @Size(max = 60, message = "最多 60 个字")
            String destination,

            @Size(max = 40, message = "最多 40 个字")
            String city,

            LocalDate startDate,
            LocalDate endDate,

            @Size(max = 20)
            String type
    ) {
    }

    public record UpdateRequest(
            @Size(max = 60, message = "最多 60 个字") String title,
            @Size(max = 60, message = "最多 60 个字") String destination,
            @Size(max = 40, message = "最多 40 个字") String city,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 20) String type
    ) {
    }

    public record JoinRequest(@NotBlank(message = "不能为空") String inviteCode) {
    }

    public record TripView(
            Long id,
            String title,
            String destination,
            String city,
            LocalDate startDate,
            LocalDate endDate,
            String type,
            String inviteCode,
            String role,
            int memberCount,
            boolean owner,
            LocalDateTime updatedAt
    ) {
    }
}
