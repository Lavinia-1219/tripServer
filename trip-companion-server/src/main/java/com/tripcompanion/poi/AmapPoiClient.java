package com.tripcompanion.poi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AmapPoiClient implements PoiClient {

    private static final String BASE_URL = "https://restapi.amap.com";

    private static final int MAX_PAGE_SIZE = 20;

    private static final String SHOW_FIELDS = "business,photos";

    private final RestClient http;
    private final String key;

    private final Map<String, CityInfo> cityCache = new ConcurrentHashMap<>();

    private record CityInfo(String adcode, PoiDtos.GeoPoint center) {
    }

    public AmapPoiClient(RestClient.Builder builder,
                         @Value("${app.poi.amap.key:}") String key) {
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
    public List<PoiDtos.PoiItem> search(PoiDtos.SearchQuery query) {
        if (!isConfigured()) {
            throw new IllegalStateException("高德 POI 未配置密钥");
        }

        int size = Math.min(Math.max(query.limit(), 1), MAX_PAGE_SIZE);

        JsonNode root = query.isKeywordSearch()
                ? keywordSearch(query, size)
                : aroundSearch(query, size);

        checkStatus(root, "高德 POI 搜索");
        return toItems(root.path("pois"));
    }

    private JsonNode keywordSearch(PoiDtos.SearchQuery q, int size) {
        String adcode = resolveCity(q.city()).adcode();
        return http.get()
                .uri(uri -> uri.path("/v5/place/text")
                        .queryParam("key", key)
                        .queryParam("keywords", q.keyword())
                        .queryParam("types", q.category().amapTypes())
                        .queryParam("region", adcode)
                        .queryParam("page_size", size)
                        .queryParam("show_fields", SHOW_FIELDS)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode aroundSearch(PoiDtos.SearchQuery q, int size) {
        if (q.center() == null) {
            String adcode = resolveCity(q.city()).adcode();
            return http.get()
                    .uri(uri -> uri.path("/v5/place/text")
                            .queryParam("key", key)
                            .queryParam("types", q.category().amapTypes())
                            .queryParam("region", adcode)
                            .queryParam("page_size", size)
                            .queryParam("show_fields", SHOW_FIELDS)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        }

        String location = q.center().longitude() + "," + q.center().latitude();
        return http.get()
                .uri(uri -> uri.path("/v5/place/around")
                        .queryParam("key", key)
                        .queryParam("types", q.category().amapTypes())
                        .queryParam("location", location)
                        .queryParam("radius", q.radiusMeters())
                        .queryParam("page_size", size)
                        .queryParam("show_fields", SHOW_FIELDS)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private List<PoiDtos.PoiItem> toItems(JsonNode pois) {
        List<PoiDtos.PoiItem> result = new ArrayList<>();
        if (pois != null && pois.isArray()) {
            for (JsonNode node : pois) {
                PoiDtos.PoiItem item = toItem(node);
                if (item != null) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    @Override
    public PoiDtos.GeoPoint geocode(String address, String city) {
        if (!isConfigured() || address == null || address.isBlank()) {
            return null;
        }
        try {
            CityInfo info = resolveCity(address);
            return info.center();
        } catch (Exception e) {
            return null;
        }
    }

    private CityInfo resolveCity(String city) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("城市名不能为空");
        }
        String cacheKey = city.trim();
        CityInfo cached = cityCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        JsonNode root = http.get()
                .uri(uri -> uri.path("/v3/geocode/geo")
                        .queryParam("key", key)
                        .queryParam("address", cacheKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        checkStatus(root, "高德地理编码");

        JsonNode geocodes = root.path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            throw new IllegalStateException("高德查不到这个地名：" + city);
        }
        JsonNode first = geocodes.get(0);
        String adcode = first.path("adcode").asText("");
        PoiDtos.GeoPoint center = parseLocation(first.path("location").asText(""));

        if (adcode.isBlank() || center == null) {
            throw new IllegalStateException("高德返回的地理编码不完整：" + city);
        }

        CityInfo info = new CityInfo(adcode, center);
        cityCache.put(cacheKey, info);
        return info;
    }

    private PoiDtos.PoiItem toItem(JsonNode node) {
        String id = node.path("id").asText("");
        String name = node.path("name").asText("");
        if (name.isBlank()) {
            return null;
        }

        PoiDtos.GeoPoint point = parseLocation(node.path("location").asText(""));
        if (point == null) {
            return null;
        }

        JsonNode business = node.path("business");

        return new PoiDtos.PoiItem(
                id,
                name,
                node.path("type").asText(""),
                node.path("typecode").asText(""),
                node.path("address").asText(""),
                point.latitude(),
                point.longitude(),
                parseRating(business.path("rating")),
                firstText(business.path("opentime_week"), business.path("opentime_today")),
                firstText(business.path("tel"), business.path("business_area")),
                firstPhoto(node.path("photos")));
    }

    private static PoiDtos.GeoPoint parseLocation(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            double lng = Double.parseDouble(parts[0].trim());
            double lat = Double.parseDouble(parts[1].trim());
            return new PoiDtos.GeoPoint(lat, lng);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseRating(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isValueNode()) {
            return null;
        }
        String text = node.asText("");
        if (text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstText(JsonNode... nodes) {
        for (JsonNode n : nodes) {
            if (n != null && n.isValueNode()) {
                String s = n.asText("");
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String firstPhoto(JsonNode photos) {
        if (photos != null && photos.isArray() && !photos.isEmpty()) {
            String url = photos.get(0).path("url").asText("");
            return url.isBlank() ? null : url;
        }
        return null;
    }

    private static void checkStatus(JsonNode root, String what) {
        if (root == null) {
            throw new IllegalStateException(what + "没有返回内容");
        }
        String status = root.path("status").asText("0");
        if (!"1".equals(status)) {
            String info = root.path("info").asText("未知错误");
            String code = root.path("infocode").asText("");
            throw new IllegalStateException(what + "失败：" + info + " (" + code + ")");
        }
    }
}
