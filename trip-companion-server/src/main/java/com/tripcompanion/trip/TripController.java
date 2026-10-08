package com.tripcompanion.trip;

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

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public ApiResponse<List<TripDtos.TripView>> list(@CurrentUser Long userId) {
        return ApiResponse.ok(tripService.listMine(userId));
    }

    @PostMapping
    public ApiResponse<TripDtos.TripView> create(@CurrentUser Long userId,
                                                 @Valid @RequestBody TripDtos.CreateRequest req) {
        return ApiResponse.ok(tripService.create(userId, req));
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripDtos.TripView> get(@CurrentUser Long userId,
                                              @PathVariable Long tripId) {
        return ApiResponse.ok(tripService.get(userId, tripId));
    }

    @PutMapping("/{tripId}")
    public ApiResponse<TripDtos.TripView> update(@CurrentUser Long userId,
                                                 @PathVariable Long tripId,
                                                 @Valid @RequestBody TripDtos.UpdateRequest req) {
        return ApiResponse.ok(tripService.update(userId, tripId, req));
    }

    @DeleteMapping("/{tripId}")
    public ApiResponse<Void> delete(@CurrentUser Long userId, @PathVariable Long tripId) {
        tripService.delete(userId, tripId);
        return ApiResponse.ok();
    }

    @PostMapping("/join")
    public ApiResponse<TripDtos.TripView> join(@CurrentUser Long userId,
                                               @Valid @RequestBody TripDtos.JoinRequest req) {
        return ApiResponse.ok(tripService.join(userId, req.inviteCode()));
    }
}
