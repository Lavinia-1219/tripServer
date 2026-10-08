package com.tripcompanion.poi;

import com.tripcompanion.trip.Trip;
import com.tripcompanion.trip.TripService;
import com.tripcompanion.user.User;
import com.tripcompanion.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class PoiService {

    private static final Logger log = LoggerFactory.getLogger(PoiService.class);

    public static final int MAX_LIMIT = 50;
    public static final int DEFAULT_LIMIT = 20;

    private final List<PoiClient> orderedClients;
    private final TripService tripService;
    private final UserRepository userRepository;
    private final PoiScorer.Weights weights;
    private final int defaultLimit;
    private final int radiusMeters;

    public PoiService(List<PoiClient> clients,
                      TripService tripService,
                      UserRepository userRepository,
                      @Value("${app.poi.order:mock,amap}") String providerOrder,
                      @Value("${app.poi.weights.preference:0.5}") double weightPreference,
                      @Value("${app.poi.weights.distance:0.3}") double weightDistance,
                      @Value("${app.poi.weights.rating:0.2}") double weightRating,
                      @Value("${app.poi.limit:20}") int defaultLimit,
                      @Value("${app.poi.radius-meters:10000}") int radiusMeters) {
        this.tripService = tripService;
        this.userRepository = userRepository;

        this.weights = new PoiScorer.Weights(weightPreference, weightDistance, weightRating);
        this.defaultLimit = clampLimit(defaultLimit);
        this.radiusMeters = Math.max(radiusMeters, 100);

        List<String> order = Arrays.stream(providerOrder.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<PoiClient> sorted = new ArrayList<>(clients);
        sorted.sort(Comparator.comparingInt(c -> {
            int index = order.indexOf(c.provider());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));
        this.orderedClients = List.copyOf(sorted);

        log.info("POI 数据源顺序: {}，权重: 偏好 {} / 距离 {} / 评分 {}",
                this.orderedClients.stream().map(PoiClient::provider).toList(),
                this.weights.preference(), this.weights.distance(), this.weights.rating());
    }

    public PoiDtos.RecommendView recommend(Long userId, Long tripId, String categoryRaw,
                                           Integer limitRaw, String keywordRaw,
                                           Double lat, Double lng, Integer radiusRaw) {
        Trip trip = tripService.requireTripForMember(tripId, userId);

        PoiDtos.Category category = PoiDtos.Category.of(categoryRaw);
        int limit = limitRaw == null ? defaultLimit : clampLimit(limitRaw);

        String city = city(trip);
        if (city == null || city.isBlank()) {
            return unavailable(tripId, "", category,
                    "这个行程还没填目的地，先去行程设置里加上城市");
        }

        Reference reference = resolveReference(city, lat, lng);

        List<String> preferences = loadPreferences(userId);
        log.debug("POI 推荐: trip={} city={} category={} 参考点={} 偏好={}",
                tripId, city, category, reference.name(), preferences);

        List<PoiDtos.PoiItem> raw = fetchFromProviders(city, category, keywordRaw, reference,
                radiusRaw == null ? radiusMeters : Math.max(radiusRaw, 100), limit);
        if (raw == null) {
            return unavailable(tripId, city, category,
                    "所有 POI 数据源都不可用（没配密钥，或外部服务暂时挂了）");
        }
        if (raw.isEmpty()) {
            return new PoiDtos.RecommendView(tripId, city, category.name(),
                    reference.name(), reference.point(), 0, weights, preferences, List.of(),
                    "在这座城市没有搜到「" + category.label() + "」类的结果");
        }

        List<PoiDtos.ScoredPoi> scored = PoiScorer.score(
                raw, reference.point(), preferences, keywordRaw, weights);

        List<PoiDtos.ScoredPoi> top = scored.size() > limit
                ? List.copyOf(scored.subList(0, limit))
                : scored;

        return new PoiDtos.RecommendView(tripId, city, category.name(),
                reference.name(), reference.point(), scored.size(), weights, preferences, top, null);
    }

    public PoiDtos.PreferenceOptions preferenceOptions() {
        return new PoiDtos.PreferenceOptions(PoiScorer.allPreferences(), PoiScorer.preferenceMapping());
    }

    private record Reference(String name, PoiDtos.GeoPoint point) {
    }

    private Reference resolveReference(String city, Double lat, Double lng) {
        if (lat != null && lng != null) {
            return new Reference("你指定的位置", new PoiDtos.GeoPoint(lat, lng));
        }

        for (PoiClient client : orderedClients) {
            if (!client.isConfigured()) {
                continue;
            }
            try {
                PoiDtos.GeoPoint center = client.geocode(city, city);
                if (center != null) {
                    return new Reference(city + "市中心", center);
                }
            } catch (Exception e) {
                log.warn("地理编码失败（{}）: {}", client.provider(), e.toString());
            }
        }

        log.warn("拿不到 {} 的坐标，距离分将全部为 0", city);
        return new Reference(city + "（坐标未知）", new PoiDtos.GeoPoint(0, 0));
    }

    private List<PoiDtos.PoiItem> fetchFromProviders(String city,
                                                     PoiDtos.Category category,
                                                     String keyword,
                                                     Reference reference,
                                                     int radius,
                                                     int limit) {
        PoiDtos.SearchQuery query = new PoiDtos.SearchQuery(
                city, category, keyword, reference.point(), radius, limit);

        boolean anyConfigured = false;
        List<String> failures = new ArrayList<>();

        for (PoiClient client : orderedClients) {
            if (!client.isConfigured()) {
                log.debug("跳过未配置的 POI 数据源: {}", client.provider());
                continue;
            }
            anyConfigured = true;

            try {
                List<PoiDtos.PoiItem> items = client.search(query);
                if (items == null) {
                    failures.add(client.provider() + ": 返回 null");
                    continue;
                }
                log.info("POI 数据源 {} 成功: city={} category={} {} 条数={}",
                        client.provider(), city, category,
                        query.isKeywordSearch() ? "关键字=" + keyword : "周边搜索",
                        items.size());
                return items;
            } catch (Exception e) {
                log.warn("POI 数据源 {} 失败: {}", client.provider(), e.toString());
                failures.add(client.provider() + ": " + e.getMessage());
            }
        }

        if (!anyConfigured) {
            log.debug("没有任何可用的 POI 数据源");
        } else {
            log.warn("所有 POI 数据源都失败了: {}", failures);
        }
        return null;
    }

    private List<String> loadPreferences(Long userId) {
        return userRepository.findById(userId)
                .map(User::getPreferences)
                .map(PoiScorer::parsePreferences)
                .orElse(List.of());
    }

    private static String city(Trip trip) {
        String c = trip.getCity();
        if (c == null || c.isBlank()) {
            c = trip.getDestination();
        }
        return c;
    }

    private static int clampLimit(int raw) {
        if (raw < 1) {
            return 1;
        }
        return Math.min(raw, MAX_LIMIT);
    }

    private PoiDtos.RecommendView unavailable(Long tripId, String city, PoiDtos.Category category,
                                             String message) {
        return new PoiDtos.RecommendView(tripId, city, category.name(),
                null, null, 0, weights, List.of(), List.of(), message);
    }
}
