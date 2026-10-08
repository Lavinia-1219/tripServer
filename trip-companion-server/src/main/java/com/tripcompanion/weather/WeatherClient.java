package com.tripcompanion.weather;
import java.util.List;
public interface WeatherClient {
    String provider();

    boolean isConfigured();

    List<WeatherDtos.DailyWeather> fetchDaily(String city,int days);
}