package com.tripcompanion.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByTripIdOrderByTriggerAtDescIdDesc(Long tripId);

    boolean existsByTicketIdAndType(Long ticketId, Reminder.Type type);

    List<Reminder> findByTicketId(Long ticketId);

    long countByTripId(Long tripId);
}
