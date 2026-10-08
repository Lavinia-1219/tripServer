package com.tripcompanion.weather;

import com.tripcompanion.trip.Trip;
import com.tripcompanion.trip.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    public static final int DEFAULT_DAYS = 3;
    public static final int MAX_DAYS = 7;

    private final List<WeatherClient> orderedClients;
    private final TripService tripService;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private final long cacheSeconds;

    public WeatherService(List<WeatherClient> clients,
                          TripService tripService,
                          @Value("${app.weather.order:qweather,amap}") String providerOrder,
                          @Value("${app.weather.cache-seconds:1800}") long cacheSeconds) {
        this.tripService = tripService;
        this.cacheSeconds = cacheSeconds;

        List<String> order = Arrays.stream(providerOrder.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<WeatherClient> sorted = new ArrayList<>(clients);
        sorted.sort(Comparator.comparingInt(c -> {
            int index = order.indexOf(c.provider());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));
        this.orderedClients = List.copyOf(sorted);
    }

    public WeatherDtos.WeatherView getWeather(String city, Integer days) {
        int dayCount = normalizeDays(days);

        if (city == null || city.isBlank()) {
            return unavailable("", "没有指定城市");
        }

        String cacheKey = city.trim() + "#" + dayCount;

        CacheEntry hit = getFromCache(cacheKey);
        if (hit != null) {
            log.debug("天气命中缓存: {}", cacheKey);
            return hit.view().withFromCache(true);
        }

        WeatherDtos.WeatherView view = fetchFromProviders(city.trim(), dayCount);

        if (view.available()) {
            putIntoCache(cacheKey, view);
        }
        return view;
    }

    public WeatherDtos.WeatherView getWeatherForTrip(Long userId, Long tripId, Integer days) {
        Trip trip = tripService.requireTripForMember(tripId, userId);

        String city = trip.getCity();
        if (city == null || city.isBlank()) {
            city = trip.getDestination();
        }
        if (city == null || city.isBlank()) {
            return unavailable("", "这个行程还没填目的地，先去行程设置里加上城市");
        }
        return getWeather(city, days);
    }

    private WeatherDtos.WeatherView fetchFromProviders(String city, int days) {
        List<String> failures = new ArrayList<>();
        boolean anyConfigured = false;

        for (WeatherClient client : orderedClients) {
            if (!client.isConfigured()) {
                log.debug("跳过未配置的数据源: {}", client.provider());
                continue;
            }
            anyConfigured = true;

            try {
                List<WeatherDtos.DailyWeather> daily = client.fetchDaily(city, days);
                if (daily == null || daily.isEmpty()) {
                    failures.add(client.provider() + ": 没返回数据");
                    continue;
                }
                log.info("天气数据源 {} 成功，城市={} 天数={}", client.provider(), city, daily.size());
                return new WeatherDtos.WeatherView(
                        city,
                        client.provider(),
                        true,
                        null,
                        false,
                        daily,
                        buildAdvice(daily));
            } catch (Exception e) {
                log.warn("天气数据源 {} 失败: {}", client.provider(), e.toString());
                failures.add(client.provider() + ": " + shorten(e.getMessage()));
            }
        }

        String reason = !anyConfigured
                ? "没有配置任何天气数据源（在环境变量里设置 AMAP_KEY 或 QWEATHER_KEY）"
                : "所有天气数据源都失败了 → " + String.join("；", failures);
        return unavailable(city, reason);
    }

    private static WeatherDtos.WeatherView unavailable(String city, String message) {
        return new WeatherDtos.WeatherView(
                city,
                null,
                false,
                message,
                false,
                List.of(),
                new WeatherDtos.Advice(null, false, false, List.of()));
    }

    private WeatherDtos.Advice buildAdvice(List<WeatherDtos.DailyWeather> days) {
        int maxTemp = days.stream()
                .filter(d -> d.tempMax() != null)
                .mapToInt(WeatherDtos.DailyWeather::tempMax)
                .max().orElse(Integer.MIN_VALUE);

        int minTemp = days.stream()
                .filter(d -> d.tempMin() != null)
                .mapToInt(WeatherDtos.DailyWeather::tempMin)
                .min().orElse(Integer.MAX_VALUE);

        boolean wet = days.stream().anyMatch(WeatherDtos.DailyWeather::looksWet);
        boolean sunny = days.stream().anyMatch(WeatherDtos.DailyWeather::looksSunny);

        List<String> tips = new ArrayList<>();
        if (wet) {
            tips.add("会下雨，建议带伞，鞋子选防滑的");
        }
        if (sunny) {
            tips.add("紫外线较强，涂防晒、戴帽子");
        }
        if (maxTemp != Integer.MIN_VALUE && minTemp != Integer.MAX_VALUE
                && maxTemp - minTemp >= 10) {
            tips.add("早晚温差有 " + (maxTemp - minTemp) + " 度，带一件可以随时穿脱的外套");
        }
        if (days.stream().anyMatch(d -> d.humidity() != null && d.humidity() >= 85)) {
            tips.add("湿度偏高，衣服选速干透气的");
        }

        String clothing;
        if (maxTemp == Integer.MIN_VALUE) {
            clothing = "温度数据缺失，建议临出门前再看一眼";
        } else if (maxTemp >= 30) {
            clothing = "很热，短袖短裤，注意补水";
        } else if (maxTemp >= 24) {
            clothing = "舒适偏热，短袖或薄长袖都行";
        } else if (maxTemp >= 18) {
            clothing = "凉快，建议长袖加一件薄外套";
        } else if (maxTemp >= 10) {
            clothing = "偏冷，外套加毛衣";
        } else if (maxTemp >= 0) {
            clothing = "冷，厚外套或羽绒服";
        } else {
            clothing = "零下，羽绒服加保暖内衣";
        }

        return new WeatherDtos.Advice(clothing, wet, sunny, List.copyOf(tips));
    }

    private record CacheEntry(WeatherDtos.WeatherView view, Instant expiresAt) {

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private CacheEntry getFromCache(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry;
    }

    private void putIntoCache(String key, WeatherDtos.WeatherView view) {
        cache.put(key, new CacheEntry(view, Instant.now().plusSeconds(cacheSeconds)));
    }

    public void clearCache() {
        cache.clear();
    }

    private static int normalizeDays(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        if (days < 1) {
            return 1;
        }
        return Math.min(days, MAX_DAYS);
    }

    private static String shorten(String message) {
        if (message == null || message.isBlank()) {
            return "未知错误";
        }
        return message.length() <= 80 ? message : message.substring(0, 80) + "...";
    }
}
