package com.tripcompanion.reminder;

import java.time.LocalDateTime;

public final class ReminderDtos {

    private ReminderDtos() {
    }

    public record ReminderView(
            Long id,
            Long ticketId,
            Long tripId,
            String type,
            String title,
            String body,
            LocalDateTime triggerAt,
            LocalDateTime createdAt
    ) {
    }

    public record ScanResult(
            LocalDateTime now,
            LocalDateTime until,
            int scanned,
            int created,
            int skipped
    ) {
    }

    public record TimeCheck(
            LocalDateTime serverTime,
            String zoneId,
            String zoneOffset,
            String epochMillis,
            long uptimeSeconds
    ) {
    }
}
