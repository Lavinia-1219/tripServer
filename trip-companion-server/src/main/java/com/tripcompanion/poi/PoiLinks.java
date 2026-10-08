package com.tripcompanion.poi;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PoiLinks {

    private PoiLinks() {
    }

    private static final String SRC = "tripcompanion";

    public record Links(
            String amap,
            String baidu,
            String meituan
    ) {
    }

    public static Links build(PoiDtos.PoiItem poi) {
        if (poi == null) {
            return new Links(null, null, null);
        }
        String name = poi.name() == null ? "" : poi.name();
        String address = poi.address() == null ? "" : poi.address();
        double lat = poi.latitude();
        double lng = poi.longitude();

        return new Links(amap(lat, lng, name), baidu(lat, lng, name, address), meituan(name));
    }

    public static String amap(double latitude, double longitude, String name) {
        return "https://uri.amap.com/marker"
                + "?position=" + longitude + "," + latitude
                + "&name=" + enc(name)
                + "&src=" + SRC;
    }

    public static String baidu(double latitude, double longitude, String name, String address) {
        return "https://api.map.baidu.com/marker"
                + "?location=" + latitude + "," + longitude
                + "&title=" + enc(name)
                + "&content=" + enc(address)
                + "&output=html"
                + "&src=webapp." + SRC;
    }

    public static String meituan(String name) {
        if (name == null || name.isBlank()) {
            return "https://i.meituan.com/";
        }
        return "https://i.meituan.com/s/" + encPath(name);
    }

    private static String enc(String s) {
        if (s == null) {
            return "";
        }
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String encPath(String s) {
        return enc(s).replace("+", "%20");
    }
}
