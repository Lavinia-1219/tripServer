package com.tripcompanion.trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trip_member",
                columnNames = {"tripId", "userId"}
        )
)
public class TripMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    protected TripMember() {
    }

    public TripMember(Long tripId, Long userId, String role) {
        this.tripId = tripId;
        this.userId = userId;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public boolean canEdit() {
        return "owner".equals(role) || "editor".equals(role);
    }
}
