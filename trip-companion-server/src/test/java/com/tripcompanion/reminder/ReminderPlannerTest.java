package com.tripcompanion.reminder;

import com.tripcompanion.ticket.Ticket;
import com.tripcompanion.ticket.TicketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReminderPlannerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 10, 0, 0);

    private static final int ADVANCE_24H = 1440;

    private static Ticket ticket(LocalDateTime departAt) {
        return new Ticket(1L, 1L, TicketType.TRAIN, "广州南", "深圳北", departAt);
    }

    private static Ticket ticketWithCode(LocalDateTime departAt) {
        Ticket t = ticket(departAt);
        t.setCode("G1234");
        return t;
    }

    @Test
    @DisplayName("2 小时后出发 → 该提醒（完成标准 1）")
    void departingInTwoHours_shouldRemind() {
        assertTrue(ReminderPlanner.shouldRemind(
                NOW.plusHours(2), NOW, ADVANCE_24H, false));
    }

    @Test
    @DisplayName("25 小时后出发 → 不提醒（还太远）")
    void departingInTwentyFiveHours_shouldNotRemind() {
        assertFalse(ReminderPlanner.shouldRemind(
                NOW.plusHours(25), NOW, ADVANCE_24H, false));
    }

    @Test
    @DisplayName("已经开走的票 → 不提醒（完成标准 3）")
    void alreadyDeparted_shouldNotRemind() {
        assertFalse(ReminderPlanner.shouldRemind(
                NOW.minusMinutes(1), NOW, ADVANCE_24H, false));
        assertFalse(ReminderPlanner.shouldRemind(
                NOW.minusDays(3), NOW, ADVANCE_24H, false));
    }

    @Test
    @DisplayName("已经提醒过的 → 不提醒（完成标准 2：幂等）")
    void alreadySent_shouldNotRemind() {
        assertFalse(ReminderPlanner.shouldRemind(
                NOW.plusHours(2), NOW, ADVANCE_24H, true));
    }

    @Test
    @DisplayName("边界：正好 24 小时 → 提醒（用 <=，含边界）")
    void exactlyAtBoundary_shouldRemind() {
        assertTrue(ReminderPlanner.shouldRemind(
                NOW.plusMinutes(ADVANCE_24H), NOW, ADVANCE_24H, false));
    }

    @Test
    @DisplayName("边界：正好「现在」→ 不提醒（用 >，不含当下那一秒）")
    void exactlyNow_shouldNotRemind() {
        assertFalse(ReminderPlanner.shouldRemind(NOW, NOW, ADVANCE_24H, false));
    }

    @Test
    @DisplayName("提前量可调：改成 60 分钟，3 小时后的票就不提醒了")
    void advanceIsConfigurable() {
        LocalDateTime depart = NOW.plusHours(3);

        assertTrue(ReminderPlanner.shouldRemind(depart, NOW, ADVANCE_24H, false),
                "按 24 小时提前量，3 小时后出发应该提醒");
        assertFalse(ReminderPlanner.shouldRemind(depart, NOW, 60, false),
                "按 60 分钟提前量，3 小时后出发不该提醒");
    }

    @Test
    @DisplayName("时间字段为 null → 不提醒，不抛异常")
    void nullTime_shouldNotRemind() {
        assertFalse(ReminderPlanner.shouldRemind(null, NOW, ADVANCE_24H, false));
        assertFalse(ReminderPlanner.shouldRemind(NOW.plusHours(1), null, ADVANCE_24H, false));
    }

    @Test
    @DisplayName("humanize：分钟级")
    void humanize_minutes() {
        assertEquals("20 分钟", ReminderPlanner.humanize(NOW, NOW.plusMinutes(20)));
        assertEquals("59 分钟", ReminderPlanner.humanize(NOW, NOW.plusMinutes(59)));
    }

    @Test
    @DisplayName("humanize：不到 1 分钟")
    void humanize_lessThanOneMinute() {
        assertEquals("不到 1 分钟", ReminderPlanner.humanize(NOW, NOW.plusSeconds(30)));
    }

    @Test
    @DisplayName("humanize：整小时不带「0 分钟」")
    void humanize_wholeHours() {
        assertEquals("2 小时", ReminderPlanner.humanize(NOW, NOW.plusHours(2)));
    }

    @Test
    @DisplayName("humanize：小时 + 分钟")
    void humanize_hoursAndMinutes() {
        assertEquals("2 小时 15 分钟", ReminderPlanner.humanize(NOW, NOW.plusMinutes(135)));
    }

    @Test
    @DisplayName("humanize：跨天用「天 小时」")
    void humanize_days() {
        assertEquals("1 天 2 小时", ReminderPlanner.humanize(NOW, NOW.plusHours(26)));
        assertEquals("1 天", ReminderPlanner.humanize(NOW, NOW.plusHours(24)));
        assertEquals("3 天", ReminderPlanner.humanize(NOW, NOW.plusHours(72)));
    }

    @Test
    @DisplayName("humanize：时间已经过去 → 空字符串（不显示负数）")
    void humanize_past() {
        assertEquals("", ReminderPlanner.humanize(NOW, NOW.minusHours(1)));
        assertEquals("", ReminderPlanner.humanize(NOW, NOW));
    }

    @Test
    @DisplayName("标题：有车次用 G 开头那个")
    void title_usesCode() {
        String title = ReminderPlanner.buildTitle(ticketWithCode(NOW.plusMinutes(135)), NOW);
        assertEquals("G1234 还有 2 小时 15 分钟出发", title);
    }

    @Test
    @DisplayName("标题：没车次就用类型中文名")
    void title_fallsBackToTypeLabel() {
        String title = ReminderPlanner.buildTitle(ticket(NOW.plusHours(2)), NOW);
        assertEquals("火车 还有 2 小时出发", title);
    }

    @Test
    @DisplayName("标题：刚好到点时不显示负数")
    void title_noNegative() {
        String title = ReminderPlanner.buildTitle(ticketWithCode(NOW), NOW);
        assertEquals("G1234 即将出发", title);
    }

    @Test
    @DisplayName("正文：出发到达 + 时间")
    void body_basic() {
        String body = ReminderPlanner.buildBody(ticketWithCode(NOW.plusMinutes(135)), NOW);

        assertTrue(body.contains("广州南 → 深圳北"), "实际：" + body);
        assertTrue(body.contains("出发时间：2026-09-25 12:15"), "实际：" + body);
    }

    @Test
    @DisplayName("正文：可选字段有就带上")
    void body_includesOptionalFields() {
        Ticket t = ticketWithCode(NOW.plusHours(2));
        t.setArriveAt(NOW.plusHours(3));
        t.setSeat("07车12F");
        t.setNote("记得带身份证");

        String body = ReminderPlanner.buildBody(t, NOW);

        assertTrue(body.contains("到达时间：2026-09-25 13:00"), "实际：" + body);
        assertTrue(body.contains("座位：07车12F"), "实际：" + body);
        assertTrue(body.contains("备注：记得带身份证"), "实际：" + body);
    }

    @Test
    @DisplayName("正文：可选字段为空时不要留空行")
    void body_skipsEmptyOptionalFields() {
        Ticket t = ticketWithCode(NOW.plusHours(2));

        String body = ReminderPlanner.buildBody(t, NOW);

        assertFalse(body.contains("到达时间"), "没有到达时间就不该出现这一行：" + body);
        assertFalse(body.contains("座位"), "没有座位就不该出现这一行：" + body);
        assertFalse(body.contains("备注"), "没有备注就不该出现这一行：" + body);
    }

    @Test
    @DisplayName("null 车票不会抛异常")
    void nullTicket_isSafe() {
        assertEquals("", ReminderPlanner.buildTitle(null, NOW));
        assertEquals("", ReminderPlanner.buildBody(null, NOW));
    }

    @Test
    @DisplayName("真实场景：2 小时后出发的 G1234，该提醒，标题和正文都对")
    void realScenario() {
        Ticket t = ticketWithCode(NOW.plusMinutes(135));
        t.setSeat("07车12F");

        assertTrue(ReminderPlanner.shouldRemind(t.getDepartAt(), NOW, ADVANCE_24H, false));
        assertEquals("G1234 还有 2 小时 15 分钟出发", ReminderPlanner.buildTitle(t, NOW));
        assertTrue(ReminderPlanner.buildBody(t, NOW).contains("广州南 → 深圳北"));
    }
}
