package com.tripcompanion.settle;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public ApiResponse<SettlementDtos.SettlementView> settlement(@CurrentUser Long userId,
                                                                 @PathVariable Long tripId) {
        return ApiResponse.ok(settlementService.settlement(userId, tripId));
    }
}