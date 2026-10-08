package com.tripcompanion.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            SELECT e FROM Expense e
            WHERE e.tripId = :tripId
              AND (:category IS NULL OR e.category = :category)
              AND (:from IS NULL OR e.spentAt >= :from)
              AND (:to IS NULL OR e.spentAt <= :to)
            ORDER BY e.spentAt DESC, e.id DESC
            """)
    List<Expense> search(@Param("tripId") Long tripId,
                         @Param("category") String category,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);

    Optional<Expense> findByIdAndTripId(Long id, Long tripId);
}