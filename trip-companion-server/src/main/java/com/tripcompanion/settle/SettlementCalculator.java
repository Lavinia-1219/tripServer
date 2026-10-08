package com.tripcompanion.settle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    public static SettlementDtos.CalculationResult calculate(List<Long> memberIds,
                                                             Map<Long, Long> paidByUser) {
        List<Long> members = memberIds.stream().distinct().sorted().toList();

        if (members.isEmpty()) {
            return new SettlementDtos.CalculationResult(List.of(), List.of());
        }

        long totalCents = 0L;
        for (Long memberId : members) {
            totalCents += paidByUser.getOrDefault(memberId, 0L);
        }

        int memberCount = members.size();
        long baseShouldPay = totalCents / memberCount;
        long remainder = totalCents % memberCount;

        List<SettlementDtos.MemberBalance> balances = new ArrayList<>(memberCount);
        List<SettlementDtos.MemberBalance> creditors = new ArrayList<>();
        List<SettlementDtos.MemberBalance> debtors = new ArrayList<>();

        for (int i = 0; i < memberCount; i++) {
            Long memberId = members.get(i);
            long paid = paidByUser.getOrDefault(memberId, 0L);
            long shouldPay = baseShouldPay + (i < remainder ? 1 : 0);
            long net = paid - shouldPay;

            SettlementDtos.MemberBalance balance =
                    new SettlementDtos.MemberBalance(memberId, paid, shouldPay, net);
            balances.add(balance);

            if (net > 0) {
                creditors.add(balance);
            } else if (net < 0) {
                debtors.add(balance);
            }
        }

        Comparator<SettlementDtos.MemberBalance> byAmountDescThenId =
                Comparator.comparingLong((SettlementDtos.MemberBalance b) -> Math.abs(b.netCents()))
                        .reversed()
                        .thenComparingLong(SettlementDtos.MemberBalance::userId);

        List<SettlementDtos.MemberBalance> creditorQueue = new ArrayList<>(creditors);
        List<SettlementDtos.MemberBalance> debtorQueue = new ArrayList<>(debtors);
        creditorQueue.sort(byAmountDescThenId);
        debtorQueue.sort(byAmountDescThenId);

        long[] creditorLeft = creditorQueue.stream().mapToLong(b -> b.netCents()).toArray();
        long[] debtorLeft = debtorQueue.stream().mapToLong(b -> -b.netCents()).toArray();

        List<SettlementDtos.Transfer> transfers = new ArrayList<>();

        int d = 0;
        int c = 0;
        while (d < debtorLeft.length && c < creditorLeft.length) {
            long amount = Math.min(debtorLeft[d], creditorLeft[c]);

            if (amount > 0) {
                transfers.add(new SettlementDtos.Transfer(
                        debtorQueue.get(d).userId(),
                        creditorQueue.get(c).userId(),
                        amount));
            }

            debtorLeft[d] -= amount;
            creditorLeft[c] -= amount;

            if (debtorLeft[d] == 0) {
                d++;
            }
            if (creditorLeft[c] == 0) {
                c++;
            }
        }

        return new SettlementDtos.CalculationResult(balances, transfers);
    }
}