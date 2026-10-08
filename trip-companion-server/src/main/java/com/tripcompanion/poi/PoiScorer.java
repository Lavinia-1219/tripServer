package com.tripcompanion.poi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PoiScorer {

    private static final Logger log = LoggerFactory.getLogger(PoiScorer.class);

    private PoiScorer() {
    }

    public record Weights(double preference, double distance, double rating, double maxDistanceMeters) {

        public static final double DEFAULT_MAX_DISTANCE_M = 10_000;

        public Weights(double preference, double distance, double rating) {
            this(preference, distance, rating, DEFAULT_MAX_DISTANCE_M);
        }

        public Weights {
            if (preference < 0 || distance < 0 || rating < 0) {
                throw new IllegalArgumentException("权重不能是负数");
            }
            if (maxDistanceMeters <= 0) {
                maxDistanceMeters = DEFAULT_MAX_DISTANCE_M;
            }
        }

        public Weights normalized() {
            double sum = preference + distance + rating;
            if (sum <= 0) {
                log.warn("POI 权重全是 0，改用默认值 0.5/0.3/0.2");
                return new Weights(0.5, 0.3, 0.2, maxDistanceMeters);
            }
            if (Math.abs(sum - 1.0) < 1e-9) {
                return this;
            }
            return new Weights(preference / sum, distance / sum, rating / sum, maxDistanceMeters);
        }
    }

    private static final Map<String, List<String>> PREFERENCE_KEYWORDS = new LinkedHashMap<>();

    static {
        PREFERENCE_KEYWORDS.put("自然风光", List.of(
                "公园", "植物园", "观景", "自然", "山", "湖", "岛", "海滩", "湿地", "森林", "瀑布"));
        PREFERENCE_KEYWORDS.put("历史人文", List.of(
                "纪念", "博物", "古迹", "遗址", "故居", "寺", "庙", "教堂", "文化", "书院", "城墙"));
        PREFERENCE_KEYWORDS.put("亲子", List.of(
                "动物园", "水族", "游乐", "科技馆", "儿童", "乐园", "海洋馆"));
        PREFERENCE_KEYWORDS.put("美食", List.of(
                "餐", "食", "菜", "小吃", "咖啡", "茶", "甜品", "烧烤", "火锅"));
        PREFERENCE_KEYWORDS.put("购物", List.of(
                "购物", "商场", "商店", "超市", "市场", "步行街", "百货"));
        PREFERENCE_KEYWORDS.put("夜生活", List.of(
                "酒吧", "夜", "KTV", "演出", "剧场", "影"));
        PREFERENCE_KEYWORDS.put("住宿", List.of(
                "酒店", "宾馆", "民宿", "旅馆", "公寓", "客栈"));
    }

    public static List<String> allPreferences() {
        return List.copyOf(PREFERENCE_KEYWORDS.keySet());
    }

    public static Map<String, List<String>> preferenceMapping() {
        return Map.copyOf(PREFERENCE_KEYWORDS);
    }

    public static List<String> parsePreferences(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(PREFERENCE_KEYWORDS::containsKey)
                .distinct()
                .toList();
    }

    private static final double EARTH_RADIUS_M = 6_371_000;

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static final double RATING_FLOOR = 3.0;
    private static final double RATING_CEIL = 5.0;

    public static double ratingScore(Double rating) {
        if (rating == null) {
            return 0.5;
        }
        return clamp((rating - RATING_FLOOR) / (RATING_CEIL - RATING_FLOOR));
    }

    public static double keywordMatchScore(PoiDtos.PoiItem poi, String keyword) {
        if (poi == null || keyword == null || keyword.isBlank()) {
            return 0.0;
        }
        String name = poi.name() == null ? "" : poi.name().toLowerCase(Locale.ROOT);
        return name.contains(keyword.trim().toLowerCase(Locale.ROOT)) ? 1.0 : 0.0;
    }

    public static double preferenceScore(PoiDtos.PoiItem poi, List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return 0.5;
        }
        String haystack = ((poi.type() == null ? "" : poi.type())
                + "|" + (poi.name() == null ? "" : poi.name())).toLowerCase(Locale.ROOT);

        for (String pref : preferences) {
            List<String> keywords = PREFERENCE_KEYWORDS.get(pref);
            if (keywords == null) {
                log.warn("不认识的偏好标签 [{}]，已忽略。可用：{}", pref, PREFERENCE_KEYWORDS.keySet());
                continue;
            }
            for (String kw : keywords) {
                if (haystack.contains(kw.toLowerCase(Locale.ROOT))) {
                    return 1.0;
                }
            }
        }
        return 0.0;
    }

    public static List<PoiDtos.ScoredPoi> score(List<PoiDtos.PoiItem> pois,
                                                PoiDtos.GeoPoint reference,
                                                List<String> preferences,
                                                Weights weights) {
        return score(pois, reference, preferences, null, weights);
    }

    public static List<PoiDtos.ScoredPoi> score(List<PoiDtos.PoiItem> pois,
                                                PoiDtos.GeoPoint reference,
                                                List<String> preferences,
                                                String keyword,
                                                Weights weights) {

        if (pois == null || pois.isEmpty()) {
            return List.of();
        }
        Weights w = weights == null ? new Weights(0.5, 0.3, 0.2).normalized() : weights.normalized();
        List<String> prefs = preferences == null ? List.of() : preferences;

        int n = pois.size();
        Double[] distances = new Double[n];
        double maxDistance = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            PoiDtos.PoiItem poi = pois.get(i);
            if (reference == null) {
                continue;
            }
            double d = distanceMeters(reference.latitude(), reference.longitude(),
                    poi.latitude(), poi.longitude());
            distances[i] = d;
            if (d > maxDistance) {
                maxDistance = d;
            }
            if (d < minDistance) {
                minDistance = d;
            }
        }

        boolean useAbsoluteDistance = minDistance < Double.MAX_VALUE
                && minDistance <= w.maxDistanceMeters();

        boolean byKeyword = keyword != null && !keyword.isBlank();
        double[] prefScores = new double[n];
        boolean anyPrefHit = false;
        for (int i = 0; i < n; i++) {
            prefScores[i] = byKeyword
                    ? keywordMatchScore(pois.get(i), keyword)
                    : preferenceScore(pois.get(i), prefs);
            if (prefScores[i] >= 1.0) {
                anyPrefHit = true;
            }
        }

        boolean prefDimensionAlive = byKeyword ? anyPrefHit : (prefs.isEmpty() || anyPrefHit);
        if (!prefDimensionAlive) {
            log.debug("匹配维度在这批结果里没有区分度（{}），降级为中立分",
                    byKeyword ? "关键字=" + keyword : "偏好=" + prefs);
        }

        List<PoiDtos.ScoredPoi> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            PoiDtos.PoiItem poi = pois.get(i);

            double pref = prefDimensionAlive ? prefScores[i] : 0.5;

            double distScore;
            Double distanceMeters = distances[i];
            if (distanceMeters == null) {
                distScore = 0.5;
            } else if (useAbsoluteDistance) {
                distScore = Math.max(0, 1.0 - distanceMeters / w.maxDistanceMeters());
            } else {
                distScore = (maxDistance - minDistance <= 0.01)
                        ? 1.0
                        : 1.0 - (distanceMeters - minDistance) / (maxDistance - minDistance);
            }

            double rat = ratingScore(poi.rating());

            double total = pref * w.preference() + distScore * w.distance() + rat * w.rating();

            out.add(new PoiDtos.ScoredPoi(
                    poi, round4(total), round4(pref), round4(distScore), round4(rat),
                    distanceMeters == null ? null : (double) Math.round(distanceMeters),
                    PoiLinks.build(poi)));
        }

        out.sort(Comparator
                .comparingDouble(PoiDtos.ScoredPoi::score).reversed()
                .thenComparing(sp -> sp.distanceMeters() == null ? Double.MAX_VALUE : sp.distanceMeters())
                .thenComparing(sp -> sp.poi().name() == null ? "" : sp.poi().name()));

        return List.copyOf(out);
    }

    public static List<PoiDtos.ScoredPoi> score(List<PoiDtos.PoiItem> pois, PoiDtos.GeoPoint reference) {
        return score(pois, reference, List.of(), new Weights(0.5, 0.3, 0.2));
    }

    private static double clamp(double v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(v, 1.0);
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
