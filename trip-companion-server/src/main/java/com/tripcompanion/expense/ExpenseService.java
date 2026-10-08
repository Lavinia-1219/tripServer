package com.tripcompanion.expense;

import com.tripcompanion.common.ApiException;
import com.tripcompanion.trip.TripMemberRepository;
import com.tripcompanion.trip.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripService tripService;
    private final TripMemberRepository memberRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          TripService tripService,
                          TripMemberRepository memberRepository) {
        this.expenseRepository = expenseRepository;
        this.tripService = tripService;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseDtos.ExpenseView> list(Long userId, Long tripId,
                                              String category, LocalDate from, LocalDate to) {
        tripService.requireTripForMember(tripId, userId);
        requireValidRange(from, to);

        return expenseRepository.search(tripId, blankToNull(category), from, to)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseDtos.SummaryView summary(Long userId, Long tripId) {
        tripService.requireTripForMember(tripId, userId);

        List<Expense> all = expenseRepository.search(tripId, null, null, null);

        long totalCents = all.stream().mapToLong(Expense::getAmountCents).sum();

        long myPaidCents = all.stream()
                .filter(e -> e.getPayerId().equals(userId))
                .mapToLong(Expense::getAmountCents)
                .sum();

        Map<String, List<Expense>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() == null ? Expense.CATEGORY_OTHER : e.getCategory()));

        List<ExpenseDtos.CategorySum> byCategory = grouped.entrySet().stream()
                .map(entry -> new ExpenseDtos.CategorySum(
                        entry.getKey(),
                        entry.getValue().stream().mapToLong(Expense::getAmountCents).sum(),
                        (long) entry.getValue().size()))
                .sorted(Comparator.comparingLong(ExpenseDtos.CategorySum::amountCents).reversed())
                .toList();

        return new ExpenseDtos.SummaryView(totalCents, (long) all.size(), myPaidCents, byCategory);
    }

    @Transactional
    public ExpenseDtos.ExpenseView create(Long userId, Long tripId, ExpenseDtos.CreateRequest req) {
        tripService.requireTripForMember(tripId, userId);
        requirePayerIsMember(tripId, req.payerId());

        Expense expense = new Expense(tripId, req.title(), req.amountCents(), req.payerId(), req.spentAt());
        expense.setCategory(req.category());
        expense.setNote(req.note());

        return toView(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseDtos.ExpenseView update(Long userId, Long tripId, Long expenseId,
                                          ExpenseDtos.UpdateRequest req) {
        tripService.requireTripForMember(tripId, userId);
        requirePayerIsMember(tripId, req.payerId());

        Expense expense = loadExpense(tripId, expenseId);

        if (req.title() != null) {
            expense.setTitle(req.title());
        }
        if (req.amountCents() != null) {
            expense.setAmountCents(req.amountCents());
        }
        if (req.payerId() != null) {
            expense.setPayerId(req.payerId());
        }
        if (req.category() != null) {
            expense.setCategory(req.category());
        }
        if (req.spentAt() != null) {
            expense.setSpentAt(req.spentAt());
        }
        if (req.note() != null) {
            expense.setNote(req.note());
        }
        expense.touch();

        return toView(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long userId, Long tripId, Long expenseId) {
        tripService.requireTripForMember(tripId, userId);
        expenseRepository.delete(loadExpense(tripId, expenseId));
    }

    private Expense loadExpense(Long tripId, Long expenseId) {
        return expenseRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> ApiException.notFound("账目不存在"));
    }

    private void requirePayerIsMember(Long tripId, Long payerId) {
        if (payerId == null) {
            return;
        }
        if (!memberRepository.existsByTripIdAndUserId(tripId, payerId)) {
            throw ApiException.badRequest("付款人不在这个行程里");
        }
    }

    private void requireValidRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("开始日期不能晚于结束日期");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private ExpenseDtos.ExpenseView toView(Expense expense) {
        return new ExpenseDtos.ExpenseView(
                expense.getId(),
                expense.getTripId(),
                expense.getTitle(),
                expense.getAmountCents(),
                expense.getPayerId(),
                expense.getCategory(),
                expense.getSpentAt(),
                expense.getNote(),
                expense.getCreatedAt(),
                expense.getUpdatedAt());
    }
}