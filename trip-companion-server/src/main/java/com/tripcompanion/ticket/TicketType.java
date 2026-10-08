package com.tripcompanion.ticket;

public enum TicketType {

    TRAIN("火车"),
    FLIGHT("飞机"),
    BUS("大巴"),
    SHIP("轮船"),
    OTHER("其他");

    private final String label;

    TicketType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static TicketType parse(String text) {
        if (text == null || text.isBlank()) {
            return OTHER;
        }
        String t = text.trim().toUpperCase();
        for (TicketType type : values()) {
            if (type.name().equals(t)) {
                return type;
            }
        }
        return OTHER;
    }
}
