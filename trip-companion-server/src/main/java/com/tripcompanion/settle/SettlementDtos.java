package com.tripcompanion.settle;

import java.util.List;

public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record MemberBalance(
            Long userId,
            Long paidCents,
            Long shouldPayCents,
            Long netCents
    ) {
    }

    public record Transfer(
            Long fromUserId,
            Long toUserId,
            Long amountCents
    ) {
    }

    public record CalculationResult(
            List<MemberBalance> balances,
            List<Transfer> transfers
    ) {
    }

    public record SettlementView(
            Long tripId,
            Long totalCents,
            Integer memberCount,
            List<MemberBalance> balances,
            List<Transfer> transfers
    ) {
    }
}