package com.tripcompanion.checklist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "checklist_rule")
public class ChecklistRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_field", nullable = false, length = 30)
    private String field;

    @Column(name = "rule_operator", nullable = false, length = 10)
    private String operator;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false, length = 50)
    private String itemName;

    @Column(length = 20)
    private String category;

    @Column(length = 120)
    private String reason;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private Boolean enabled = true;

    protected ChecklistRule() {
    }

    public ChecklistRule(String field, String operator, Double threshold, String itemName) {
        this.field = field;
        this.operator = operator;
        this.threshold = threshold;
        this.itemName = itemName;
    }

    public Long getId() {
        return id;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
