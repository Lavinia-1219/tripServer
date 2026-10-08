package com.tripcompanion.poi;

import com.tripcompanion.common.ApiException;

import java.util.List;
import java.util.Locale;

public final class PoiDtos {

    private PoiDtos() {
    }

    public record GeoPoint(double latitude, double longitude) {
    }

    public enum Category {
        ATTRACTION("景点", "110000"),
        HOTEL("酒店", "100000"),
        FOOD("餐厅", "050000");

        private final String label;
        private final String amapTypes;

        Category(String label, String amapTypes) {
            this.label = label;
            this.amapTypes = amapTypes;
        }

        public String label() {
            return label;
        }

        public String amapTypes() {
            return amapTypes;
        }

        public static Category of(String raw) {
            if (raw == null || raw.isBlank()) {
                throw ApiException.badRequest("BAD_CATEGORY",
                        "缺少类别参数。可用：" + choices());
            }
            String s = raw.trim().toLowerCase(Locale.ROOT);
            for (Category c : values()) {
                if (c.name().toLowerCase(Locale.ROOT).equals(s) || c.label.equals(s)) {
                    return c;
                }
            }
            throw ApiException.badRequest("BAD_CATEGORY",
                    "不认识的类别：" + raw + "。可用：" + choices());
        }

        public static String choices() {
            StringBuilder sb = new StringBuilder();
            for (Category c : values()) {
                if (sb.length() > 0) {
                    sb.append(" / ");
                }
                sb.append(c.name().toLowerCase(Locale.ROOT)).append('(').append(c.label).append(')');
            }
            return sb.toString();
        }
    }

    public record PoiItem(
            String id,
            String name,
            String type,
            String typecode,
            String address,
            double latitude,
            double longitude,
            Double rating,
            String opentime,
            String tel,
            String photoUrl
    ) {
    }

    public record SearchQuery(
            String city,
            Category category,
            String keyword,
            GeoPoint center,
            int radiusMeters,
            int limit
    ) {
        public boolean isKeywordSearch() {
            return keyword != null && !keyword.isBlank();
        }

        public boolean isAroundSearch() {
            return !isKeywordSearch() && center != null;
        }
    }

    public record ScoredPoi(
            PoiItem poi,
            double score,
            double preferenceScore,
            double distanceScore,
            double ratingScore,
            Double distanceMeters,
            PoiLinks.Links links
    ) {
    }

    public record RecommendView(
            Long tripId,
            String city,
            String category,
            String referenceName,
            GeoPoint reference,
            int total,
            PoiScorer.Weights weights,
            List<String> preferences,
            List<ScoredPoi> items,
            String message
    ) {
    }

    public record PreferenceRequest(
            String preferences
    ) {
    }

    public record PreferenceOptions(
            List<String> all,
            java.util.Map<String, List<String>> mapping
    ) {
    }
}
