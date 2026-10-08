package com.tripcompanion.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ticket",
        indexes = {
                @Index(name = "idx_ticket_depart", columnList = "depart_at"),
                @Index(name = "idx_ticket_trip", columnList = "trip_id")
        }
)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 20)
    private TicketType type = TicketType.OTHER;

    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "from_station", nullable = false, length = 60)
    private String fromStation;

    @Column(name = "to_station", nullable = false, length = 60)
    private String toStation;

    @Column(name = "depart_at", nullable = false)
    private LocalDateTime departAt;

    @Column(name = "arrive_at")
    private LocalDateTime arriveAt;

    @Column(name = "seat", length = 40)
    private String seat;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Ticket() {
    }

    public Ticket(Long tripId, Long ownerId, TicketType type,
                  String fromStation, String toStation, LocalDateTime departAt) {
        this.tripId = tripId;
        this.ownerId = ownerId;
        this.type = type == null ? TicketType.OTHER : type;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.departAt = departAt;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFromStation() {
        return fromStation;
    }

    public void setFromStation(String fromStation) {
        this.fromStation = fromStation;
    }

    public String getToStation() {
        return toStation;
    }

    public void setToStation(String toStation) {
        this.toStation = toStation;
    }

    public LocalDateTime getDepartAt() {
        return departAt;
    }

    public void setDepartAt(LocalDateTime departAt) {
        this.departAt = departAt;
    }

    public LocalDateTime getArriveAt() {
        return arriveAt;
    }

    public void setArriveAt(LocalDateTime arriveAt) {
        this.arriveAt = arriveAt;
    }

    public String getSeat() {
        return seat;
    }

    public void setSeat(String seat) {
        this.seat = seat;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
