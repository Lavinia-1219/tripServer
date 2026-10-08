package com.tripcompanion.checklist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistRuleRepository extends JpaRepository<ChecklistRule, Long> {

    List<ChecklistRule> findByEnabledTrueOrderByPriorityAscIdAsc();

    List<ChecklistRule> findAllByOrderByPriorityAscIdAsc();
}
