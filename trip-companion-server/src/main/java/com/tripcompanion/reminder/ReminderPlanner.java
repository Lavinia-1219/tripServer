package com.tripcompanion.reminder;

import com.tripcompanion.ticket.Ticket;
import com.tripcompanion.ticket.TicketType;

import java.time.Duration;
import java.time.LocalDateTime;

public final class ReminderPlanner {

    private ReminderPlanner() {
    }

    public static boolean shouldRemind(LocalDateTime departAt,
                                       LocalDateTime now,
                                       int advanceMinutes,
                                       boolean alreadySent) {

        if (departAt == null || now == null) {
            return false;
        }
        if (alreadySent) {
            return false;
        }

        if (!departAt.isAfter(now)) {
            return false;
        }

        LocalDateTime until = now.plusMinutes(advanceMinutes);
        return !departAt.isAfter(until);
    }

    public static String buildTitle(Ticket ticket, LocalDateTime now) {
        if (ticket == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        String who = (ticket.getCode() != null && !ticket.getCode().isBlank())
                ? ticket.getCode()
                : label(ticket.getType());

        sb.append(who);

        String left = humanize(now, ticket.getDepartAt());
        if (!left.isBlank()) {
            sb.append(" 还有 ").append(left).append("出发");
        } else {
            sb.append(" 即将出发");
        }
        return sb.toString();
    }

    public static String buildBody(Ticket ticket, LocalDateTime now) {
        if (ticket == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        sb.append(nz(ticket.getFromStation()))
                .append(" → ")
                .append(nz(ticket.getToStation()))
                .append('\n');

        sb.append("出发时间：").append(format(ticket.getDepartAt()));

        if (ticket.getArriveAt() != null) {
            sb.append('\n').append("到达时间：").append(format(ticket.getArriveAt()));
        }
        if (ticket.getSeat() != null && !ticket.getSeat().isBlank()) {
            sb.append('\n').append("座位：").append(ticket.getSeat().trim());
        }
        if (ticket.getNote() != null && !ticket.getNote().isBlank()) {
            sb.append('\n').append("备注：").append(ticket.getNote().trim());
        }
        return sb.toString();
    }

    public static String humanize(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return "";
        }
        Duration d = Duration.between(from, to);
        if (d.isNegative() || d.isZero()) {
            return "";
        }

        long totalMinutes = d.toMinutes();

        if (totalMinutes < 1) {
            return "不到 1 分钟";
        }
        if (totalMinutes < 60) {
            return totalMinutes + " 分钟";
        }

        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes % (60 * 24)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return hours > 0 ? days + " 天 " + hours + " 小时" : days + " 天";
        }
        return minutes > 0 ? hours + " 小时 " + minutes + " 分钟" : hours + " 小时";
    }

    private static String label(TicketType type) {
        return type == null ? "行程" : type.label();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String format(LocalDateTime t) {
        if (t == null) {
            return "";
        }
        return String.format("%04d-%02d-%02d %02d:%02d",
                t.getYear(), t.getMonthValue(), t.getDayOfMonth(),
                t.getHour(), t.getMinute());
    }
}
