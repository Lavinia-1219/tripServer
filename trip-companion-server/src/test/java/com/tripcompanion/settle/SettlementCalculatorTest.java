package com.tripcompanion.settle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementCalculatorTest {

    private void assertNetSumIsZero(SettlementDtos.CalculationResult result) {
        long sum = result.balances().stream()
                .mapToLong(SettlementDtos.MemberBalance::netCents)
                .sum();
        assertThat(sum)
                .as("所有净额相加必须为 0（否则说明有钱凭空产生或消失）")
                .isZero();
    }

    private void assertPlanSettlesEveryone(SettlementDtos.CalculationResult result) {
        Map<Long, Long> delta = new HashMap<>();
        for (SettlementDtos.MemberBalance b : result.balances()) {
            delta.put(b.userId(), b.netCents());
        }

        for (SettlementDtos.Transfer t : result.transfers()) {
            delta.merge(t.fromUserId(), t.amountCents(), Long::sum);
            delta.merge(t.toUserId(), -t.amountCents(), Long::sum);
        }

        for (Map.Entry<Long, Long> e : delta.entrySet()) {
            assertThat(e.getValue())
                    .as("按方案转账后，用户 %s 应该归零", e.getKey())
                    .isZero();
        }
    }

    @Test
    @DisplayName("两人：A 垫 5050，B 垫 6000，总额 11050 → A 该给 B 转 475")
    void twoPeople() {
        List<Long> members = List.of(1L, 2L);
        Map<Long, Long> paid = Map.of(1L, 5050L, 2L, 6000L);

        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(members, paid);

        assertThat(result.balances()).hasSize(2);
        assertThat(result.balances().get(0).userId()).isEqualTo(1L);
        assertThat(result.balances().get(0).paidCents()).isEqualTo(5050L);
        assertThat(result.balances().get(0).shouldPayCents()).isEqualTo(5525L);
        assertThat(result.balances().get(0).netCents()).isEqualTo(-475L);

        assertThat(result.balances().get(1).netCents()).isEqualTo(475L);

        assertThat(result.transfers()).hasSize(1);
        SettlementDtos.Transfer t = result.transfers().get(0);
        assertThat(t.fromUserId()).isEqualTo(1L);
        assertThat(t.toUserId()).isEqualTo(2L);
        assertThat(t.amountCents()).isEqualTo(475L);

        assertNetSumIsZero(result);
        assertPlanSettlesEveryone(result);
    }

    @Test
    @DisplayName("三人：一个人全垫，另外两人各还一份")
    void threePeopleOnePaidAll() {
        List<Long> members = List.of(1L, 2L, 3L);
        Map<Long, Long> paid = Map.of(1L, 10000L, 2L, 0L, 3L, 0L);

        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(members, paid);

        assertThat(result.balances().get(0).shouldPayCents()).isEqualTo(3334L);
        assertThat(result.balances().get(1).shouldPayCents()).isEqualTo(3333L);
        assertThat(result.balances().get(2).shouldPayCents()).isEqualTo(3333L);

        long sumShouldPay = result.balances().stream()
                .mapToLong(SettlementDtos.MemberBalance::shouldPayCents)
                .sum();
        assertThat(sumShouldPay).isEqualTo(10000L);

        assertThat(result.transfers()).hasSize(2);
        assertThat(result.transfers())
                .allMatch(t -> t.toUserId().equals(1L));

        assertNetSumIsZero(result);
        assertPlanSettlesEveryone(result);
    }

    @Test
    @DisplayName("边界：只有 1 个人 → 自己跟自己不用转账")
    void singlePerson() {
        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(List.of(1L), Map.of(1L, 8888L));

        assertThat(result.balances()).hasSize(1);
        assertThat(result.balances().get(0).netCents()).isZero();
        assertThat(result.transfers()).isEmpty();
        assertNetSumIsZero(result);
    }

    @Test
    @DisplayName("边界：一分钱都没花 → 谁都不欠谁")
    void noExpenses() {
        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(List.of(1L, 2L, 3L), Map.of());

        assertThat(result.transfers()).isEmpty();
        assertThat(result.balances())
                .allMatch(b -> b.netCents() == 0L);
        assertNetSumIsZero(result);
    }

    @Test
    @DisplayName("边界：没有成员 → 返回空，不报错")
    void noMembers() {
        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(List.of(), Map.of());

        assertThat(result.balances()).isEmpty();
        assertThat(result.transfers()).isEmpty();
    }

    @Test
    @DisplayName("边界：每人垫的钱刚好等于该承担的 → 不用转账")
    void perfectlyEven() {
        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(List.of(1L, 2L), Map.of(1L, 1000L, 2L, 1000L));

        assertThat(result.transfers()).isEmpty();
        assertNetSumIsZero(result);
    }

    @Test
    @DisplayName("边界：100 分 3 人除不尽 → 分摊之和仍然精确等于 100")
    void remainderMustNotVanish() {
        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(List.of(1L, 2L, 3L), Map.of(1L, 100L));

        long sumShouldPay = result.balances().stream()
                .mapToLong(SettlementDtos.MemberBalance::shouldPayCents)
                .sum();

        assertThat(sumShouldPay)
                .as("100 分分给 3 个人，应承担之和必须还是 100")
                .isEqualTo(100L);

        assertNetSumIsZero(result);
        assertPlanSettlesEveryone(result);
    }

    @Test
    @DisplayName("边界：1 分钱 3 人 → 只有一个人承担这 1 分")
    void oneCentThreePeople() {
        SettlementDtos.CalculationResult result =
                SettlementCalculator.calculate(List.of(1L, 2L, 3L), Map.of(1L, 1L));

        assertThat(result.balances().get(0).shouldPayCents()).isEqualTo(1L);
        assertThat(result.balances().get(1).shouldPayCents()).isZero();

        assertNetSumIsZero(result);
        assertPlanSettlesEveryone(result);
    }

    @Test
    @DisplayName("不变量：几十组不同的账目组合，统统满足守恒 + 能平账")
    void invariantsHoldAcrossManyInputs() {
        Random random = new Random(42);

        for (int round = 0; round < 200; round++) {
            int peopleCount = 1 + random.nextInt(8);
            List<Long> members = new ArrayList<>();
            Map<Long, Long> paid = new HashMap<>();

            for (long id = 1; id <= peopleCount; id++) {
                members.add(id);
                if (random.nextInt(4) != 0) {
                    paid.put(id, (long) random.nextInt(50000));
                }
            }

            SettlementDtos.CalculationResult result =
                    SettlementCalculator.calculate(members, paid);

            assertNetSumIsZero(result);
            assertPlanSettlesEveryone(result);
        }
    }

    @Test
    @DisplayName("稳定性：同样的输入调两次，结果必须完全一样")
    void resultIsDeterministic() {
        List<Long> members = List.of(3L, 1L, 2L);
        Map<Long, Long> paid = Map.of(1L, 3333L, 2L, 1111L, 3L, 7777L);

        SettlementDtos.CalculationResult first =
                SettlementCalculator.calculate(members, paid);
        SettlementDtos.CalculationResult second =
                SettlementCalculator.calculate(members, paid);

        assertThat(first.balances()).isEqualTo(second.balances());
        assertThat(first.transfers()).isEqualTo(second.transfers());
    }
}