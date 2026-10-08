package com.tripcompanion.weather;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AmapWeatherClient implements WeatherClient {

    private static final String BASE_URL = "https://restapi.amap.com";
    private static final int MAX_FORECAST_DAYS = 4;

    private final RestClient http;
    private final String key;

    private final Map<String, String> adcodeCache = new ConcurrentHashMap<>();

    public AmapWeatherClient(RestClient.Builder builder,
                             @Value("${app.weather.amap.key:}") String key) {
        this.key = key == null ? "" : key.trim();
        this.http = builder.clone().baseUrl(BASE_URL).build();
    }

    @Override
    public String provider() {
        return "amap";
    }

    @Override
    public boolean isConfigured() {
        return !key.isBlank();
    }

    @Override
    public List<WeatherDtos.DailyWeather> fetchDaily(String city, int days) {
        if (!isConfigured()) {
            throw new IllegalStateException("高德天气未配置密钥");
        }

        String adcode = resolveAdcode(city);
        JsonNode root = http.get()
                .uri(uri -> uri.path("/v3/weather/weatherInfo")
                        .queryParam("city", adcode)
                        .queryParam("key", key)
                        .queryParam("extensions", "all")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        checkStatus(root);

        JsonNode casts = root.path("forecasts").path(0).path("casts");
        if (!casts.isArray() || casts.isEmpty()) {
            throw new IllegalStateException("高德天气没返回预报数据");
        }

        int wanted = Math.min(days, MAX_FORECAST_DAYS);
        List<WeatherDtos.DailyWeather> result = new ArrayList<>();
        for (JsonNode cast : casts) {
            Integer dayTemp = number(cast, "daytemp");
            Integer nightTemp = number(cast, "nighttemp");

            result.add(new WeatherDtos.DailyWeather(
                    parseDate(text(cast, "date")),
                    text(cast, "dayweather"),
                    text(cast, "nightweather"),
                    min(dayTemp, nightTemp),
                    max(dayTemp, nightTemp),
                    number(cast, "dayhumidity"),
                    text(cast, "daywind"),
                    text(cast, "daypower"),
                    provider()));

            if (result.size() >= wanted) {
                break;
            }
        }
        return result;
    }

    private String resolveAdcode(String city) {
        return adcodeCache.computeIfAbsent(city, name -> {
            JsonNode root = http.get()
                    .uri(uri -> uri.path("/v3/config/district")
                            .queryParam("keywords", name)
                            .queryParam("key", key)
                            .queryParam("subdistrict", 0)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            checkStatus(root);

            JsonNode districts = root.path("districts");
            if (!districts.isArray() || districts.isEmpty()) {
                throw new IllegalStateException("高德查不到城市：" + name);
            }

            JsonNode picked = districts.get(0);
            for (JsonNode district : districts) {
                if ("city".equals(district.path("level").asText())) {
                    picked = district;
                    break;
                }
            }
            return picked.path("adcode").asText();
        });
    }

    private static void checkStatus(JsonNode root) {
        if (root == null) {
            throw new IllegalStateException("高德天气返回空响应");
        }
        String status = root.path("status").asText();
        if (!"1".equals(status)) {
            throw new IllegalStateException("高德接口返回失败："
                    + root.path("info").asText("未知错误")
                    + "（infocode=" + root.path("infocode").asText("?") + "）");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String raw = value.asText();
        return (raw == null || raw.isBlank()) ? null : raw;
    }

    private static Integer number(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer max(Integer a, Integer b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.max(a, b);
    }

    private static Integer min(Integer a, Integer b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.min(a, b);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
