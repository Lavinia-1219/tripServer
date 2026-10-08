package com.tripcompanion.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByTripIdOrderByDepartAtAsc(Long tripId);

    Optional<Ticket> findByIdAndTripId(Long id, Long tripId);

    @Query("""
            select t from Ticket t
            where t.reminderSentAt is null
              and t.departAt > :from
              and t.departAt <= :until
            order by t.departAt asc
            """)
    List<Ticket> findRemindable(@Param("from") LocalDateTime from,
                                @Param("until") LocalDateTime until);

    long countByTripIdAndReminderSentAtIsNull(Long tripId);
}
