package com.tripcompanion.ticket;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ApiResponse<TicketDtos.TripTicketsView> list(@CurrentUser Long userId,
                                                        @PathVariable Long tripId) {
        return ApiResponse.ok(ticketService.list(userId, tripId));
    }

    @PostMapping
    public ApiResponse<TicketDtos.TicketView> create(@CurrentUser Long userId,
                                                     @PathVariable Long tripId,
                                                     @Valid @RequestBody TicketDtos.CreateRequest req) {
        return ApiResponse.ok(ticketService.create(userId, tripId, req));
    }

    @PutMapping("/{ticketId}")
    public ApiResponse<TicketDtos.TicketView> update(@CurrentUser Long userId,
                                                     @PathVariable Long tripId,
                                                     @PathVariable Long ticketId,
                                                     @Valid @RequestBody TicketDtos.UpdateRequest req) {
        return ApiResponse.ok(ticketService.update(userId, tripId, ticketId, req));
    }

    @DeleteMapping("/{ticketId}")
    public ApiResponse<Void> delete(@CurrentUser Long userId,
                                    @PathVariable Long tripId,
                                    @PathVariable Long ticketId) {
        ticketService.delete(userId, tripId, ticketId);
        return ApiResponse.ok();
    }
}
