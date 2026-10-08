package com.tripcompanion.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reminder",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reminder_ticket_type",
                columnNames = {"ticket_id", "reminder_type"}
        ),
        indexes = @Index(name = "idx_reminder_trip", columnList = "trip_id")
)
public class Reminder {

    public enum Type {
        DEPART_SOON
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 30)
    private Type type;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    @Column(name = "trigger_at", nullable = false)
    private LocalDateTime triggerAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Reminder() {
    }

    public Reminder(Long ticketId, Long tripId, Type type,
                    String title, String body, LocalDateTime triggerAt) {
        this.ticketId = ticketId;
        this.tripId = tripId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.triggerAt = triggerAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Long getTripId() {
        return tripId;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getTriggerAt() {
        return triggerAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
