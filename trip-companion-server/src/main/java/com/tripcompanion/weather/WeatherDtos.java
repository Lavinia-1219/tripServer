package com.tripcompanion.weather;

import java.time.LocalDate;
import java.util.List;

public final class WeatherDtos {

    private WeatherDtos() {
    }

    public record DailyWeather(
            LocalDate date,
            String textDay,
            String textNight,
            Integer tempMin,
            Integer tempMax,
            Integer humidity,
            String windDir,
            String windScale,
            String provider
    ) {

        public boolean looksWet() {
            return containsAny(textDay, "雨", "雪", "雷", "冰雹", "冻雨")
                    || containsAny(textNight, "雨", "雪", "雷", "冰雹", "冻雨");
        }

        public boolean looksSunny() {
            return containsAny(textDay, "晴", "少云") && tempMax != null && tempMax >= 26;
        }

        private static boolean containsAny(String text, String... keywords) {
            if (text == null || text.isBlank()) {
                return false;
            }
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }

    public record Advice(
            String clothing,
            boolean needUmbrella,
            boolean needSunscreen,
            List<String> tips
    ) {
    }

    public record WeatherView(
            String city,
            String provider,
            boolean available,
            String message,
            boolean fromCache,
            List<DailyWeather> days,
            Advice advice
    ) {

        public WeatherView withFromCache(boolean cached) {
            return new WeatherView(city, provider, available, message, cached, days, advice);
        }
    }
}