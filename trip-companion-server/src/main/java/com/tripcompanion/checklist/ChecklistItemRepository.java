package com.tripcompanion.checklist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByTripIdOrderByIdAsc(Long tripId);

    List<ChecklistItem> findByTripIdAndStatusOrderByIdAsc(Long tripId, String status);

    Optional<ChecklistItem> findByIdAndTripId(Long id, Long tripId);
}