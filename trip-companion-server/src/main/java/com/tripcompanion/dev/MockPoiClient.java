package com.tripcompanion.dev;

import com.tripcompanion.poi.PoiClient;
import com.tripcompanion.poi.PoiDtos;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("dev")
public class MockPoiClient implements PoiClient {

    private static final double BASE_LAT = 23.1065;
    private static final double BASE_LNG = 113.3245;

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public boolean isConfigured() {
        return enabled.get();
    }

    @Override
    public List<PoiDtos.PoiItem> search(PoiDtos.SearchQuery query) {
        List<PoiDtos.PoiItem> all = switch (query.category()) {
            case ATTRACTION -> attractions();
            case HOTEL -> hotels();
            case FOOD -> foods();
        };

        if (query.isKeywordSearch()) {
            String kw = query.keyword().trim();
            all = all.stream().filter(p -> p.name().contains(kw)).toList();
        }

        int limit = query.limit();
        return all.size() > limit ? List.copyOf(all.subList(0, limit)) : all;
    }

    @Override
    public PoiDtos.GeoPoint geocode(String address, String city) {
        return new PoiDtos.GeoPoint(BASE_LAT, BASE_LNG);
    }

    private static List<PoiDtos.PoiItem> attractions() {
        List<PoiDtos.PoiItem> list = new ArrayList<>();
        list.add(poi("a1", "云台花园", 0.002, 0.002, 4.8, "风景名胜;公园广场;公园", "自然风光 命中：公园"));
        list.add(poi("a2", "某某纪念馆", 0.003, 0.003, 4.7, "风景名胜;风景名胜;纪念馆", "历史人文 命中：纪念"));
        list.add(poi("a3", "老街步行街", 0.020, 0.020, 4.6, "购物服务;特色商业街", "购物 命中：步行街"));
        list.add(poi("a4", "普通小公园", 0.001, 0.001, 3.2, "风景名胜;公园广场;公园", "最近，但评分低"));
        list.add(poi("a5", "远方大景区", 0.090, 0.090, 5.0, "风景名胜;风景名胜;国家级景点", "最远，但满分"));
        list.add(poi("a6", "街边小花园", 0.004, 0.004, 4.0, "风景名胜;公园广场;公园", "中等"));
        list.add(poi("a7", "城市博物馆", 0.006, 0.006, 4.5, "科教文化服务;博物馆", "历史人文 命中：博物"));
        list.add(poi("a8", "午夜酒吧街", 0.008, 0.008, 4.1, "娱乐场所;酒吧", "夜生活 命中：酒吧"));
        return list;
    }

    private static List<PoiDtos.PoiItem> hotels() {
        List<PoiDtos.PoiItem> list = new ArrayList<>();
        list.add(poi("h1", "江景大酒店", 0.003, 0.003, 4.7, "住宿服务;宾馆酒店;五星级宾馆", "近 + 高分"));
        list.add(poi("h2", "市中心民宿", 0.001, 0.001, 4.3, "住宿服务;旅馆招待所;民宿", "最近"));
        list.add(poi("h3", "经济连锁酒店", 0.012, 0.012, 3.9, "住宿服务;宾馆酒店;经济型连锁酒店", "便宜"));
        list.add(poi("h4", "远郊度假村", 0.070, 0.070, 4.9, "住宿服务;宾馆酒店;度假村", "最远 + 高分"));
        return list;
    }

    private static List<PoiDtos.PoiItem> foods() {
        List<PoiDtos.PoiItem> list = new ArrayList<>();
        list.add(poi("f1", "老字号茶楼", 0.002, 0.002, 4.8, "餐饮服务;中餐厅;粤菜", "近 + 高分 + 命中「美食」"));
        list.add(poi("f2", "网红火锅店", 0.005, 0.005, 4.6, "餐饮服务;中餐厅;火锅店", "命中「火锅」"));
        list.add(poi("f3", "街角咖啡", 0.001, 0.001, 4.2, "餐饮服务;咖啡厅", "最近 + 命中「咖啡」"));
        list.add(poi("f4", "普通快餐", 0.003, 0.003, 3.1, "餐饮服务;快餐厅", "评分最低"));
        list.add(poi("f5", "远方农庄", 0.060, 0.060, 4.9, "餐饮服务;中餐厅;农家菜", "最远 + 高分"));
        return list;
    }

    private static PoiDtos.PoiItem poi(String id, String name, double dLat, double dLng,
                                       double rating, String type, String note) {
        return new PoiDtos.PoiItem(
                "mock-" + id,
                name,
                type,
                "999999",
                "假数据地址（" + note + "）",
                BASE_LAT + dLat,
                BASE_LNG + dLng,
                rating,
                "周一至周日 09:00-22:00",
                "000-00000000",
                null);
    }
}
