package com.tripcompanion.reminder;

import com.tripcompanion.ticket.Ticket;
import com.tripcompanion.ticket.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final TicketRepository ticketRepository;
    private final ReminderRepository reminderRepository;

    private final int advanceMinutes;

    public ReminderService(TicketRepository ticketRepository,
                           ReminderRepository reminderRepository,
                           @Value("${app.reminder.advance-minutes:1440}") int advanceMinutes) {
        this.ticketRepository = ticketRepository;
        this.reminderRepository = reminderRepository;
        this.advanceMinutes = advanceMinutes;
    }

    @Transactional
    public ReminderDtos.ScanResult scan(LocalDateTime now) {
        LocalDateTime until = now.plusMinutes(advanceMinutes);

        List<Ticket> candidates = ticketRepository.findRemindable(now, until);

        int created = 0;
        int skipped = 0;

        for (Ticket ticket : candidates) {
            if (reminderRepository.existsByTicketIdAndType(ticket.getId(), Reminder.Type.DEPART_SOON)) {
                skipped++;
                log.debug("车票 {} 已有提醒记录，跳过", ticket.getId());
                continue;
            }

            Reminder reminder = new Reminder(
                    ticket.getId(),
                    ticket.getTripId(),
                    Reminder.Type.DEPART_SOON,
                    ReminderPlanner.buildTitle(ticket, now),
                    ReminderPlanner.buildBody(ticket, now),
                    now);

            reminderRepository.save(reminder);

            ticket.setReminderSentAt(now);
            ticketRepository.save(ticket);

            created++;
        }

        ReminderDtos.ScanResult result =
                new ReminderDtos.ScanResult(now, until, candidates.size(), created, skipped);

        if (created > 0 || skipped > 0) {
            log.info("提醒扫描完成：候选 {} 张，新建 {} 条，跳过 {} 条（窗口 {} ~ {}）",
                    result.scanned(), result.created(), result.skipped(), now, until);
        } else {
            log.debug("提醒扫描完成：本周期没有需要提醒的车票");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReminderDtos.ReminderView> listByTrip(Long tripId) {
        return reminderRepository.findByTripIdOrderByTriggerAtDescIdDesc(tripId)
                .stream()
                .map(ReminderService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countByTrip(Long tripId) {
        return reminderRepository.countByTripId(tripId);
    }

    @Transactional
    public void deleteForTicket(Long ticketId) {
        List<Reminder> old = reminderRepository.findByTicketId(ticketId);
        if (!old.isEmpty()) {
            reminderRepository.deleteAll(old);
            log.info("车票 {} 的 {} 条提醒已随票删除", ticketId, old.size());
        }
    }

    private static ReminderDtos.ReminderView toView(Reminder r) {
        return new ReminderDtos.ReminderView(
                r.getId(),
                r.getTicketId(),
                r.getTripId(),
                r.getType() == null ? null : r.getType().name(),
                r.getTitle(),
                r.getBody(),
                r.getTriggerAt(),
                r.getCreatedAt());
    }
}
