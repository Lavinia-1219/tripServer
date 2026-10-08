package com.tripcompanion.dev;

import com.tripcompanion.weather.WeatherClient;
import com.tripcompanion.weather.WeatherDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile("dev")
public class MockWeatherClient implements WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(MockWeatherClient.class);

    private volatile String scenario;

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public boolean isConfigured() {
        return scenario != null;
    }

    public void setScenario(String scenario) {
        this.scenario = (scenario == null || scenario.isBlank())
                ? null
                : scenario.trim().toLowerCase(Locale.ROOT);
        log.warn("【仅开发环境】假天气场景已设为: {}", this.scenario);
    }

    public String getScenario() {
        return scenario;
    }

    @Override
    public List<WeatherDtos.DailyWeather> fetchDaily(String city, int days) {
        String current = scenario;
        if (current == null) {
            return List.of();
        }

        int dayCount = Math.max(1, days);
        List<WeatherDtos.DailyWeather> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < dayCount; i++) {
            list.add(day(today.plusDays(i), current, i));
        }
        return list;
    }

    private static WeatherDtos.DailyWeather day(LocalDate date, String scenario, int index) {
        return switch (scenario) {
            case "rain" -> new WeatherDtos.DailyWeather(
                    date, "中雨", "小雨", 18, 24, 90, "东南风", "3-4级", "mock");

            case "hot" -> new WeatherDtos.DailyWeather(
                    date, "晴", "晴", 28, 36, 40, "南风", "1-2级", "mock");

            case "snow" -> new WeatherDtos.DailyWeather(
                    date, "小雪", "中雪", -5, 1, 75, "北风", "4-5级", "mock");

            case "humid" -> new WeatherDtos.DailyWeather(
                    date, "阴", "阴", 20, 26, 92, "东南风", "2-3级", "mock");

            case "mixed" -> switch (index % 3) {
                case 0 -> new WeatherDtos.DailyWeather(
                        date, "大雨", "中雨", 16, 21, 95, "东风", "3-4级", "mock");
                case 1 -> new WeatherDtos.DailyWeather(
                        date, "晴", "多云", 27, 34, 45, "南风", "1-2级", "mock");
                default -> new WeatherDtos.DailyWeather(
                        date, "雨夹雪", "小雪", -2, 4, 80, "北风", "4-5级", "mock");
            };

            default -> new WeatherDtos.DailyWeather(
                    date, "多云", "多云", 20, 28, 55, "无持续风向", "1-2级", "mock");
        };
    }

    public static List<String> supportedScenarios() {
        return List.of("rain", "hot", "snow", "humid", "mixed");
    }
}
