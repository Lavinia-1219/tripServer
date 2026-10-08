package com.tripcompanion.checklist;

import com.tripcompanion.weather.WeatherDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineTest {

    private static final LocalDate D1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 10, 2);
    private static final LocalDate D3 = LocalDate.of(2026, 10, 3);

    private static WeatherDtos.DailyWeather day(LocalDate date, String textDay,
                                                Integer tempMin, Integer tempMax,
                                                Integer humidity) {
        return new WeatherDtos.DailyWeather(
                date, textDay, textDay, tempMin, tempMax, humidity, "无持续风向", "1-2级", "test");
    }

    private static WeatherDtos.DailyWeather rainy(LocalDate date) {
        return day(date, "中雨", 16, 21, 90);
    }

    private static WeatherDtos.DailyWeather hotSunny(LocalDate date) {
        return day(date, "晴", 28, 36, 40);
    }

    private static WeatherDtos.DailyWeather cold(LocalDate date) {
        return day(date, "多云", 3, 9, 50);
    }

    private static ChecklistRule rule(String field, String op, double threshold,
                                      String item, int priority) {
        ChecklistRule r = new ChecklistRule(field, op, threshold, item);
        r.setPriority(priority);
        r.setEnabled(true);
        return r;
    }

    private static List<ChecklistRule> defaultRules() {
        return ChecklistRuleSeeder.defaultRules();
    }

    @Test
    @DisplayName("下雨天 → 建议里有雨伞")
    void rainyDay_suggestsUmbrella() {
        List<RuleEngine.Hit> hits = RuleEngine.evaluate(
                List.of(rainy(D1), rainy(D2), rainy(D3)),
                defaultRules(),
                List.of());

        assertTrue(hits.stream().anyMatch(h -> h.itemName().equals("雨伞")),
                "下雨天应该建议带雨伞，实际：" + names(hits));
    }

    @Test
    @DisplayName("高温晴天 → 建议里没有雨伞，但有防晒相关")
    void hotDay_noUmbrella() {
        List<RuleEngine.Hit> hits = RuleEngine.evaluate(
                List.of(hotSunny(D1), hotSunny(D2)),
                defaultRules(),
                List.of());

        assertFalse(hits.stream().anyMatch(h -> h.itemName().equals("雨伞")),
                "大晴天不该建议带伞，实际：" + names(hits));
        assertTrue(hits.stream().anyMatch(h -> h.itemName().equals("防晒霜")),
                "高温晴天应该建议防晒霜，实际：" + names(hits));
    }

    @Test
    @DisplayName("清单里已经有雨伞 → 不再重复建议")
    void existingItem_isNotSuggestedAgain() {
        List<RuleEngine.Hit> hits = RuleEngine.evaluate(
                List.of(rainy(D1)),
                defaultRules(),
                List.of("雨伞"));

        assertFalse(hits.stream().anyMatch(h -> h.itemName().equals("雨伞")),
                "清单里已有雨伞，不该再建议，实际：" + names(hits));
    }

    @Test
    @DisplayName("去重是「归一化」的：带空格、大小写不同也算同一个")
    void existingItem_dedupIgnoresSpacesAndCase() {
        List<RuleEngine.Hit> hits = RuleEngine.evaluate(
                List.of(rainy(D1)),
                defaultRules(),
                List.of("雨 伞 "));

        assertFalse(hits.stream().anyMatch(h -> h.itemName().equals("雨伞")),
                "「雨 伞」和「雨伞」应该算同一个东西，实际：" + names(hits));
    }

    @Test
    @DisplayName("运行时加一条新规则 → 立刻生效（不用改 Java 代码）")
    void newRuleTakesEffectImmediately() {
        List<WeatherDtos.DailyWeather> days = List.of(day(D1, "阴", 20, 26, 92));

        List<RuleEngine.Hit> before = RuleEngine.evaluate(days, defaultRules(), List.of());

        List<ChecklistRule> rules = new java.util.ArrayList<>(defaultRules());
        rules.add(rule("humidity", "GE", 90, "除湿袋", 200));

        List<RuleEngine.Hit> after = RuleEngine.evaluate(days, rules, List.of());

        assertFalse(before.stream().anyMatch(h -> h.itemName().equals("除湿袋")),
                "加规则之前不该有除湿袋");
        assertTrue(after.stream().anyMatch(h -> h.itemName().equals("除湿袋")),
                "加规则之后应该有除湿袋，实际：" + names(after));
    }

    @Test
    @DisplayName("天气不可用（空列表）→ 返回空建议，不抛异常")
    void noWeather_returnsEmpty() {
        assertEquals(List.of(), RuleEngine.evaluate(List.of(), defaultRules(), List.of()));
        assertEquals(List.of(), RuleEngine.evaluate(null, defaultRules(), List.of()));
        assertEquals(List.of(), RuleEngine.evaluate(List.of(rainy(D1)), null, List.of()));
        assertEquals(List.of(), RuleEngine.evaluate(List.of(rainy(D1)), List.of(), List.of()));
    }

    @Test
    @DisplayName("阈值是「闭区间」：LE 10 时 10 命中，11 不命中")
    void thresholdIsInclusive() {
        ChecklistRule coat = rule("tempMin", "LE", 10, "厚外套", 20);

        assertTrue(RuleEngine.matches(coat, day(D1, "多云", 10, 18, 50)),
                "tempMin = 10，LE 10 应该命中");
        assertFalse(RuleEngine.matches(coat, day(D1, "多云", 11, 18, 50)),
                "tempMin = 11，LE 10 不该命中");
        assertTrue(RuleEngine.matches(coat, day(D1, "多云", 0, 18, 50)),
                "tempMin = 0，当然命中");
    }

    @Test
    @DisplayName("六种比较符都对")
    void allOperatorsWork() {
        WeatherDtos.DailyWeather d = day(D1, "多云", 15, 25, 60);

        assertTrue(RuleEngine.matches(rule("tempMax", "EQ", 25, "x", 1), d));
        assertFalse(RuleEngine.matches(rule("tempMax", "EQ", 24, "x", 1), d));

        assertTrue(RuleEngine.matches(rule("tempMax", "NE", 24, "x", 1), d));
        assertTrue(RuleEngine.matches(rule("tempMax", "GT", 24, "x", 1), d));
        assertFalse(RuleEngine.matches(rule("tempMax", "GT", 25, "x", 1), d));
        assertTrue(RuleEngine.matches(rule("tempMax", "GE", 25, "x", 1), d));
        assertTrue(RuleEngine.matches(rule("tempMax", "LT", 26, "x", 1), d));
        assertTrue(RuleEngine.matches(rule("tempMax", "LE", 25, "x", 1), d));
        assertFalse(RuleEngine.matches(rule("tempMax", "LE", 24, "x", 1), d));
    }

    @Test
    @DisplayName("比较符大小写不敏感：ge 和 GE 一样")
    void operatorIsCaseInsensitive() {
        WeatherDtos.DailyWeather d = day(D1, "多云", 15, 30, 60);
        assertTrue(RuleEngine.matches(rule("tempMax", "ge", 30, "x", 1), d));
    }

    @Test
    @DisplayName("条件字段不认识 → 这条规则跳过，其他规则照常生效")
    void unknownField_isSkippedNotCrash() {
        List<ChecklistRule> rules = List.of(
                rule("这只字段不存在", "EQ", 1, "莫名其妙的东西", 1),
                rule("wet", "EQ", 1, "雨伞", 2));

        List<RuleEngine.Hit> hits = RuleEngine.evaluate(List.of(rainy(D1)), rules, List.of());

        assertEquals(1, hits.size(), "坏规则应该被跳过，好规则应该照常生效");
        assertEquals("雨伞", hits.get(0).itemName());
    }

    @Test
    @DisplayName("比较符不认识 → 跳过，不抛异常")
    void unknownOperator_isSkippedNotCrash() {
        List<ChecklistRule> rules = List.of(rule("tempMax", "大概等于", 30, "帽子", 1));
        assertEquals(List.of(), RuleEngine.evaluate(List.of(hotSunny(D1)), rules, List.of()));
    }

    @Test
    @DisplayName("天气里没有这个数据（humidity = null）→ 跳过，不拿 0 去比")
    void nullWeatherValue_isSkipped() {
        WeatherDtos.DailyWeather noHumidity = day(D1, "阴", 20, 26, null);
        ChecklistRule humidRule = rule("humidity", "GE", 85, "速干衣", 1);

        assertFalse(RuleEngine.matches(humidRule, noHumidity),
                "湿度缺失时不能当成 0 或 100，应该直接跳过");
        assertEquals(List.of(), RuleEngine.evaluate(List.of(noHumidity), List.of(humidRule), List.of()));
    }

    @Test
    @DisplayName("阈值是 null → 跳过")
    void nullThreshold_isSkipped() {
        ChecklistRule bad = new ChecklistRule("tempMax", "GE", null, "帽子");
        assertFalse(RuleEngine.matches(bad, hotSunny(D1)));
    }

    @Test
    @DisplayName("停用的规则不参与匹配")
    void disabledRule_isIgnored() {
        ChecklistRule umbrella = rule("wet", "EQ", 1, "雨伞", 1);
        umbrella.setEnabled(false);

        assertEquals(List.of(), RuleEngine.evaluate(List.of(rainy(D1)), List.of(umbrella), List.of()));
    }

    @Test
    @DisplayName("同一个物品被多条规则命中 → 只出现一次，原因合并、日期合并")
    void sameItemFromTwoRules_isMerged() {
        ChecklistRule byTemp = rule("tempMin", "LE", 10, "厚外套", 1);
        byTemp.setReason("最低温 ≤10℃");
        ChecklistRule byRain = rule("wet", "EQ", 1, "厚外套", 2);
        byRain.setReason("下雨降温");

        List<RuleEngine.Hit> hits = RuleEngine.evaluate(
                List.of(day(D1, "中雨", 8, 14, 80)),
                List.of(byTemp, byRain),
                List.of());

        assertEquals(1, hits.size(), "同一个物品不该出现两条，实际：" + names(hits));
        assertEquals("厚外套", hits.get(0).itemName());
        assertTrue(hits.get(0).reason().contains("最低温"),
                "原因应该合并，实际：" + hits.get(0).reason());
        assertTrue(hits.get(0).reason().contains("下雨"),
                "原因应该合并，实际：" + hits.get(0).reason());
    }

    @Test
    @DisplayName("同一条规则多天命中 → 建议只出现一次，但 dates 有全部命中的日子")
    void multiDayHit_keepsAllDates() {
        List<RuleEngine.Hit> hits = RuleEngine.evaluate(
                List.of(rainy(D1), hotSunny(D2), rainy(D3)),
                List.of(rule("wet", "EQ", 1, "雨伞", 1)),
                List.of());

        assertEquals(1, hits.size());
        assertEquals(List.of(D1, D3), hits.get(0).dates(),
                "10/1 和 10/3 都有雨，应该都记下来");
    }

    @Test
    @DisplayName("建议顺序跟着规则顺序走（规则已按优先级排好）")
    void orderFollowsRuleOrder() {
        List<ChecklistRule> rules = List.of(
                rule("wet", "EQ", 1, "雨伞", 10),
                rule("tempMin", "LE", 10, "厚外套", 20),
                rule("humidity", "GE", 85, "速干衣", 30));

        List<RuleEngine.Hit> hits = RuleEngine.evaluate(
                List.of(day(D1, "中雨", 8, 14, 90)),
                rules,
                List.of());

        assertEquals(List.of("雨伞", "厚外套", "速干衣"), names(hits));
    }

    @Test
    @DisplayName("describe 能把规则读成人话")
    void describe_isReadable() {
        assertEquals("最低温 ≤ 10 → 厚外套",
                RuleEngine.describe(rule("tempMin", "LE", 10, "厚外套", 1)));
        assertEquals("有雨雪 = 1 → 雨伞",
                RuleEngine.describe(rule("wet", "EQ", 1, "雨伞", 1)));
        assertEquals("湿度 ≥ 85.5 → 速干衣",
                RuleEngine.describe(rule("humidity", "GE", 85.5, "速干衣", 1)));
    }

    @Test
    @DisplayName("默认规则表里每一条的字段和比较符都是引擎认识的（防止手滑写错）")
    void defaultRulesAreAllValid() {
        for (ChecklistRule r : defaultRules()) {
            assertTrue(RuleEngine.supportedFields().contains(r.getField()),
                    "默认规则里有引擎不认识的字段：" + r.getField());
            assertTrue(RuleEngine.supportedOperators().contains(r.getOperator()),
                    "默认规则里有引擎不认识的比较符：" + r.getOperator());
            assertTrue(r.getItemName() != null && !r.getItemName().isBlank(),
                    "默认规则里有没写物品名的");
            assertTrue(Boolean.TRUE.equals(r.getEnabled()), "默认规则应该都是启用的");
        }
    }

    @Test
    @DisplayName("默认规则条数 >= 8（别被人不小心删光了）")
    void defaultRulesNotEmpty() {
        assertTrue(defaultRules().size() >= 8, "默认规则太少了：" + defaultRules().size());
    }

    private static List<String> names(List<RuleEngine.Hit> hits) {
        return hits.stream().map(RuleEngine.Hit::itemName).toList();
    }
}
