package com.tripcompanion.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense")
public class Expense {

    public static final String CATEGORY_OTHER = "other";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false)
    private Long amountCents;

    @Column(nullable = false)
    private Long payerId;

    @Column(length = 20)
    private String category;

    @Column(nullable = false)
    private LocalDate spentAt;

    @Column(length = 200)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Expense() {
    }

    public Expense(Long tripId, String title, Long amountCents, Long payerId, LocalDate spentAt) {
        this.tripId = tripId;
        this.title = title;
        this.amountCents = amountCents;
        this.payerId = payerId;
        this.spentAt = spentAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public Long getPayerId() {
        return payerId;
    }

    public void setPayerId(Long payerId) {
        this.payerId = payerId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getSpentAt() {
        return spentAt;
    }

    public void setSpentAt(LocalDate spentAt) {
        this.spentAt = spentAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}