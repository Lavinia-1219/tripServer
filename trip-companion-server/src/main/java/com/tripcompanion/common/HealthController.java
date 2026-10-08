package com.tripcompanion.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final boolean weatherKeyConfigured;
    private final List<String> weatherConfiguredProviders;

    public HealthController(@Value("${app.weather.qweather.key:}") String qweatherKey,
                            @Value("${app.weather.amap.key:}") String amapKey) {
        List<String> providers = new ArrayList<>();
        if (!qweatherKey.isBlank()) {
            providers.add("qweather");
        }
        if (!amapKey.isBlank()) {
            providers.add("amap");
        }
        this.weatherConfiguredProviders = List.copyOf(providers);
        this.weatherKeyConfigured = !providers.isEmpty();
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("time", LocalDateTime.now().toString());
        body.put("weatherProviders", weatherConfiguredProviders);
        body.put("weatherConfigured", weatherKeyConfigured);
        return ApiResponse.ok(body);
    }
}
