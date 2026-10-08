package com.tripcompanion.ticket;

import com.tripcompanion.common.ApiException;
import com.tripcompanion.reminder.ReminderDtos;
import com.tripcompanion.reminder.ReminderService;
import com.tripcompanion.trip.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final TripService tripService;
    private final ReminderService reminderService;

    public TicketService(TicketRepository ticketRepository,
                         TripService tripService,
                         ReminderService reminderService) {
        this.ticketRepository = ticketRepository;
        this.tripService = tripService;
        this.reminderService = reminderService;
    }

    @Transactional(readOnly = true)
    public TicketDtos.TripTicketsView list(Long userId, Long tripId) {
        tripService.requireTripForMember(tripId, userId);

        LocalDateTime now = LocalDateTime.now();

        List<TicketDtos.TicketView> tickets = ticketRepository.findByTripIdOrderByDepartAtAsc(tripId)
                .stream()
                .map(t -> toView(t, now))
                .toList();

        List<ReminderDtos.ReminderView> reminders = reminderService.listByTrip(tripId);

        return new TicketDtos.TripTicketsView(tripId, tickets, reminders);
    }

    @Transactional
    public TicketDtos.TicketView create(Long userId, Long tripId, TicketDtos.CreateRequest req) {
        tripService.requireTripForMember(tripId, userId);

        requireArriveAfterDepart(req.departAt(), req.arriveAt());

        Ticket ticket = new Ticket(
                tripId,
                userId,
                TicketType.parse(req.type()),
                req.fromStation().trim(),
                req.toStation().trim(),
                req.departAt());

        ticket.setCode(trimToNull(req.code()));
        ticket.setArriveAt(req.arriveAt());
        ticket.setSeat(trimToNull(req.seat()));
        ticket.setNote(trimToNull(req.note()));

        Ticket saved = ticketRepository.save(ticket);
        log.info("行程 {} 新增车票 {}：{} → {} 出发时间 {}",
                tripId, saved.getId(), saved.getFromStation(), saved.getToStation(), saved.getDepartAt());

        return toView(saved, LocalDateTime.now());
    }

    @Transactional
    public TicketDtos.TicketView update(Long userId, Long tripId, Long ticketId,
                                        TicketDtos.UpdateRequest req) {
        tripService.requireTripForMember(tripId, userId);

        Ticket ticket = loadTicket(tripId, ticketId);

        if (req.type() != null) {
            ticket.setType(TicketType.parse(req.type()));
        }
        if (req.code() != null) {
            ticket.setCode(trimToNull(req.code()));
        }
        if (req.fromStation() != null && !req.fromStation().isBlank()) {
            ticket.setFromStation(req.fromStation().trim());
        }
        if (req.toStation() != null && !req.toStation().isBlank()) {
            ticket.setToStation(req.toStation().trim());
        }
        if (req.arriveAt() != null) {
            ticket.setArriveAt(req.arriveAt());
        }
        if (req.seat() != null) {
            ticket.setSeat(trimToNull(req.seat()));
        }
        if (req.note() != null) {
            ticket.setNote(trimToNull(req.note()));
        }

        if (req.departAt() != null && !req.departAt().equals(ticket.getDepartAt())) {
            requireArriveAfterDepart(req.departAt(), req.arriveAt() != null ? req.arriveAt() : ticket.getArriveAt());

            log.info("行程 {} 车票 {} 出发时间 {} → {}，作废旧提醒",
                    tripId, ticketId, ticket.getDepartAt(), req.departAt());

            ticket.setDepartAt(req.departAt());
            reminderService.deleteForTicket(ticketId);
            ticket.setReminderSentAt(null);
        }

        ticket.touch();
        return toView(ticketRepository.save(ticket), LocalDateTime.now());
    }

    @Transactional
    public void delete(Long userId, Long tripId, Long ticketId) {
        tripService.requireTripForMember(tripId, userId);

        Ticket ticket = loadTicket(tripId, ticketId);

        reminderService.deleteForTicket(ticketId);
        ticketRepository.delete(ticket);

        log.info("行程 {} 删除车票 {}", tripId, ticketId);
    }

    private Ticket loadTicket(Long tripId, Long ticketId) {
        return ticketRepository.findByIdAndTripId(ticketId, tripId)
                .orElseThrow(() -> ApiException.notFound("车票不存在，或者不属于这个行程"));
    }

    private static void requireArriveAfterDepart(LocalDateTime departAt, LocalDateTime arriveAt) {
        if (departAt == null || arriveAt == null) {
            return;
        }
        if (arriveAt.isBefore(departAt)) {
            throw ApiException.badRequest("BAD_TIME_RANGE", "到达时间不能早于出发时间");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    static TicketDtos.TicketView toView(Ticket t, LocalDateTime now) {
        return new TicketDtos.TicketView(
                t.getId(),
                t.getTripId(),
                t.getOwnerId(),
                t.getType() == null ? null : t.getType().name(),
                t.getType() == null ? null : t.getType().label(),
                t.getCode(),
                t.getFromStation(),
                t.getToStation(),
                t.getDepartAt(),
                t.getArriveAt(),
                t.getSeat(),
                t.getNote(),
                t.getReminderSentAt(),
                t.getReminderSentAt() != null,
                com.tripcompanion.reminder.ReminderPlanner.humanize(now, t.getDepartAt()),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
