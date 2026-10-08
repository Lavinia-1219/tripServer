package com.tripcompanion.dev;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import com.tripcompanion.common.ApiException;
import com.tripcompanion.weather.WeatherService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/weather")
public class MockWeatherController {

    private final MockWeatherClient mockClient;
    private final WeatherService weatherService;

    public MockWeatherController(MockWeatherClient mockClient, WeatherService weatherService) {
        this.mockClient = mockClient;
        this.weatherService = weatherService;
    }

    @PostMapping("/scenario")
    public ApiResponse<Map<String, Object>> set(@CurrentUser Long userId,
                                                @RequestParam String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase();

        if (!MockWeatherClient.supportedScenarios().contains(normalized)) {
            throw ApiException.badRequest("BAD_SCENARIO",
                    "不认识的场景：" + name + "。可用：" + MockWeatherClient.supportedScenarios());
        }

        mockClient.setScenario(normalized);
        return ApiResponse.ok(state("已开启假天气场景"));
    }

    @DeleteMapping("/scenario")
    public ApiResponse<Map<String, Object>> clear(@CurrentUser Long userId) {
        mockClient.setScenario(null);
        return ApiResponse.ok(state("已关闭假天气，回到真实数据源"));
    }

    @GetMapping("/scenario")
    public ApiResponse<Map<String, Object>> current(@CurrentUser Long userId) {
        return ApiResponse.ok(state("当前假天气状态"));
    }

    @DeleteMapping("/cache")
    public ApiResponse<Map<String, Object>> clearCache(@CurrentUser Long userId) {
        weatherService.clearCache();
        return ApiResponse.ok(state("已清空天气缓存"));
    }

    private Map<String, Object> state(String note) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("note", note);
        body.put("scenario", mockClient.getScenario());
        body.put("active", mockClient.isConfigured());
        body.put("supported", MockWeatherClient.supportedScenarios());
        return body;
    }
}
