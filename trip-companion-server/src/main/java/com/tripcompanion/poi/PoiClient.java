package com.tripcompanion.poi;

import java.util.List;

public interface PoiClient {

    String provider();

    boolean isConfigured();

    List<PoiDtos.PoiItem> search(PoiDtos.SearchQuery query);

    PoiDtos.GeoPoint geocode(String address, String city);
}
