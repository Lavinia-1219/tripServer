package com.tripcompanion.settle;

import com.tripcompanion.expense.Expense;
import com.tripcompanion.expense.ExpenseRepository;
import com.tripcompanion.trip.TripMember;
import com.tripcompanion.trip.TripMemberRepository;
import com.tripcompanion.trip.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettlementService {

    private final ExpenseRepository expenseRepository;
    private final TripMemberRepository memberRepository;
    private final TripService tripService;

    public SettlementService(ExpenseRepository expenseRepository,
                             TripMemberRepository memberRepository,
                             TripService tripService) {
        this.expenseRepository = expenseRepository;
        this.memberRepository = memberRepository;
        this.tripService = tripService;
    }

    @Transactional(readOnly = true)
    public SettlementDtos.SettlementView settlement(Long userId, Long tripId) {
        tripService.requireTripForMember(tripId, userId);

        List<Long> memberIds = memberRepository.findByTripId(tripId).stream()
                .map(TripMember::getUserId)
                .toList();

        List<Expense> expenses = expenseRepository.search(tripId, null, null, null);

        Map<Long, Long> paidByUser = new HashMap<>();
        for (Long memberId : memberIds) {
            paidByUser.put(memberId, 0L);
        }
        for (Expense expense : expenses) {
            paidByUser.merge(expense.getPayerId(), expense.getAmountCents(), Long::sum);
        }

        long totalCents = memberIds.stream()
                .mapToLong(id -> paidByUser.getOrDefault(id, 0L))
                .sum();

        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(memberIds, paidByUser);

        return new SettlementDtos.SettlementView(
                tripId,
                totalCents,
                memberIds.size(),
                result.balances(),
                result.transfers());
    }
}