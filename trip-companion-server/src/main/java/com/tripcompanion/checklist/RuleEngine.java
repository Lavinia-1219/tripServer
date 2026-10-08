package com.tripcompanion.checklist;

import com.tripcompanion.weather.WeatherDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public final class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private static final Map<String, Function<WeatherDtos.DailyWeather, Double>> EXTRACTORS = Map.of(
            "wet", day -> day.looksWet() ? 1.0 : 0.0,
            "sunny", day -> day.looksSunny() ? 1.0 : 0.0,
            "tempMax", day -> toDouble(day.tempMax()),
            "tempMin", day -> toDouble(day.tempMin()),
            "humidity", day -> toDouble(day.humidity())
    );

    private RuleEngine() {
    }

    public static Set<String> supportedFields() {
        return EXTRACTORS.keySet();
    }

    public static Set<String> supportedOperators() {
        return Set.of("EQ", "NE", "GT", "GE", "LT", "LE");
    }

    public record Hit(
            String itemName,
            String category,
            String reason,
            List<LocalDate> dates
    ) {
    }

    public static List<Hit> evaluate(List<WeatherDtos.DailyWeather> days,
                                     List<ChecklistRule> rules,
                                     Collection<String> existingNames) {

        if (days == null || days.isEmpty() || rules == null || rules.isEmpty()) {
            return List.of();
        }

        Set<String> existing = new HashSet<>();
        if (existingNames != null) {
            for (String name : existingNames) {
                existing.add(normalize(name));
            }
        }

        Map<String, Hit> hits = new LinkedHashMap<>();

        for (ChecklistRule rule : rules) {
            if (rule == null || !Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }

            String itemKey = normalize(rule.getItemName());
            if (itemKey.isEmpty()) {
                continue;
            }

            if (existing.contains(itemKey)) {
                continue;
            }

            List<LocalDate> matchedDates = new ArrayList<>();
            for (WeatherDtos.DailyWeather day : days) {
                if (day == null) {
                    continue;
                }
                if (matches(rule, day)) {
                    matchedDates.add(day.date());
                }
            }
            if (matchedDates.isEmpty()) {
                continue;
            }

            merge(hits, itemKey, rule, matchedDates);
        }

        return List.copyOf(hits.values());
    }

    public static List<Hit> evaluate(List<WeatherDtos.DailyWeather> days,
                                     List<ChecklistRule> rules) {
        return evaluate(days, rules, List.of());
    }

    public static boolean matches(ChecklistRule rule, WeatherDtos.DailyWeather day) {
        if (rule == null || day == null) {
            return false;
        }

        Function<WeatherDtos.DailyWeather, Double> extractor = EXTRACTORS.get(rule.getField());
        if (extractor == null) {
            log.warn("规则 {} 的条件字段 [{}] 不认识，已跳过。可用字段：{}",
                    rule.getId(), rule.getField(), EXTRACTORS.keySet());
            return false;
        }

        Double value = extractor.apply(day);
        if (value == null) {
            return false;
        }
        if (rule.getThreshold() == null) {
            log.warn("规则 {} 没有配阈值，已跳过", rule.getId());
            return false;
        }

        if (!isKnownOperator(rule.getOperator())) {
            log.warn("规则 {} 的比较符 [{}] 不认识，已跳过。可用：{}",
                    rule.getId(), rule.getOperator(), supportedOperators());
            return false;
        }

        return compare(value, rule.getOperator(), rule.getThreshold());
    }

    private static boolean compare(double value, String operator, double threshold) {
        if (!isKnownOperator(operator)) {
            return false;
        }
        return switch (operator.trim().toUpperCase(Locale.ROOT)) {
            case "EQ" -> Double.compare(value, threshold) == 0;
            case "NE" -> Double.compare(value, threshold) != 0;
            case "GT" -> value > threshold;
            case "GE" -> value >= threshold;
            case "LT" -> value < threshold;
            case "LE" -> value <= threshold;
            default -> false;
        };
    }

    private static boolean isKnownOperator(String operator) {
        return operator != null && supportedOperators().contains(operator.trim().toUpperCase(Locale.ROOT));
    }

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "wet", "有雨雪",
            "sunny", "晴天高温",
            "tempMax", "最高温",
            "tempMin", "最低温",
            "humidity", "湿度"
    );

    private static final Map<String, String> OPERATOR_LABELS = Map.of(
            "EQ", "=",
            "NE", "≠",
            "GT", ">",
            "GE", "≥",
            "LT", "<",
            "LE", "≤"
    );

    public static String describe(ChecklistRule rule) {
        if (rule == null) {
            return "";
        }
        String field = FIELD_LABELS.getOrDefault(rule.getField(), rule.getField());
        String operator = OPERATOR_LABELS.getOrDefault(
                rule.getOperator() == null ? "" : rule.getOperator().trim().toUpperCase(Locale.ROOT),
                rule.getOperator());
        String threshold = rule.getThreshold() == null ? "?" : trimNumber(rule.getThreshold());
        return field + " " + operator + " " + threshold + " → " + rule.getItemName();
    }

    private static String trimNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static void merge(Map<String, Hit> hits, String itemKey,
                              ChecklistRule rule, List<LocalDate> matchedDates) {

        Hit old = hits.get(itemKey);
        if (old == null) {
            hits.put(itemKey, new Hit(
                    rule.getItemName(),
                    rule.getCategory(),
                    rule.getReason(),
                    List.copyOf(matchedDates)));
            return;
        }

        Set<LocalDate> allDates = new TreeSet<>();
        for (LocalDate d : old.dates()) {
            if (d != null) {
                allDates.add(d);
            }
        }
        for (LocalDate d : matchedDates) {
            if (d != null) {
                allDates.add(d);
            }
        }

        String reason = old.reason() == null ? "" : old.reason();
        String extra = rule.getReason() == null ? "" : rule.getReason();
        if (!extra.isBlank() && !reason.contains(extra)) {
            reason = reason.isBlank() ? extra : reason + "；" + extra;
        }

        String category = old.category() != null ? old.category() : rule.getCategory();

        hits.put(itemKey, new Hit(old.itemName(), category,
                reason.isBlank() ? null : reason, List.copyOf(allDates)));
    }

    private static Double toDouble(Integer value) {
        return value == null ? null : value.doubleValue();
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
