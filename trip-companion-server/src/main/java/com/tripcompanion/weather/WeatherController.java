package com.tripcompanion.weather;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/api/weather")
    public ApiResponse<WeatherDtos.WeatherView> byCity(
            @RequestParam String city,
            @RequestParam(required = false) Integer days) {
        return ApiResponse.ok(weatherService.getWeather(city, days));
    }

    @GetMapping("/api/trips/{tripId}/weather")
    public ApiResponse<WeatherDtos.WeatherView> byTrip(
            @CurrentUser Long userId,
            @PathVariable Long tripId,
            @RequestParam(required = false) Integer days) {
        return ApiResponse.ok(weatherService.getWeatherForTrip(userId, tripId, days));
    }
}
