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
public class QWeatherClient implements WeatherClient {

    private static final int[] SUPPORTED_DAYS = {3, 7, 10, 15, 30};

    private final RestClient apiHttp;
    private final RestClient geoHttp;
    private final String key;

    private final Map<String, String> locationIdCache = new ConcurrentHashMap<>();

    public QWeatherClient(RestClient.Builder builder,
                          @Value("${app.weather.qweather.key:}") String key,
                          @Value("${app.weather.qweather.host:devapi.qweather.com}") String apiHost,
                          @Value("${app.weather.qweather.geo-host:geoapi.qweather.com}") String geoHost) {
        this.key = key == null ? "" : key.trim();
        this.apiHttp = builder.clone().baseUrl("https://" + apiHost).build();
        this.geoHttp = builder.clone().baseUrl("https://" + geoHost).build();
    }

    @Override
    public String provider() {
        return "qweather";
    }

    @Override
    public boolean isConfigured() {
        return !key.isBlank();
    }

    @Override
    public List<WeatherDtos.DailyWeather> fetchDaily(String city, int days) {
        if (!isConfigured()) {
            throw new IllegalStateException("和风天气未配置密钥");
        }

        String locationId = resolveLocationId(city);
        JsonNode root = apiHttp.get()
                .uri(uri -> uri.path("/v7/weather/{range}")
                        .queryParam("location", locationId)
                        .queryParam("key", key)
                        .queryParam("lang", "zh")
                        .build(pickRange(days)))
                .retrieve()
                .body(JsonNode.class);

        JsonNode daily = root == null ? null : root.get("daily");
        if (daily == null || !daily.isArray() || daily.isEmpty()) {
            String code = root == null ? "null" : root.path("code").asText("?");
            throw new IllegalStateException("和风天气没返回预报数据，code=" + code);
        }

        List<WeatherDtos.DailyWeather> result = new ArrayList<>();
        for (JsonNode day : daily) {
            result.add(new WeatherDtos.DailyWeather(
                    parseDate(day.path("fxDate").asText(null)),
                    text(day, "textDay"),
                    text(day, "textNight"),
                    number(day, "tempMin"),
                    number(day, "tempMax"),
                    number(day, "humidity"),
                    text(day, "windDirDay"),
                    text(day, "windScaleDay"),
                    provider()));
            if (result.size() >= days) {
                break;
            }
        }
        return result;
    }

    private String resolveLocationId(String city) {
        return locationIdCache.computeIfAbsent(city, name -> {
            JsonNode root = geoHttp.get()
                    .uri(uri -> uri.path("/v2/city/lookup")
                            .queryParam("location", name)
                            .queryParam("key", key)
                            .queryParam("lang", "zh")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode locations = root == null ? null : root.get("location");
            if (locations == null || !locations.isArray() || locations.isEmpty()) {
                throw new IllegalStateException("和风天气查不到城市：" + name);
            }
            return locations.get(0).path("id").asText();
        });
    }

    private static int pickRange(int wanted) {
        for (int supported : SUPPORTED_DAYS) {
            if (wanted <= supported) {
                return supported;
            }
        }
        return SUPPORTED_DAYS[SUPPORTED_DAYS.length - 1];
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private static Integer number(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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
