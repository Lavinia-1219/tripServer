package com.tripcompanion.poi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiLinksTest {

    private static final double LAT = 23.1065;
    private static final double LNG = 113.3245;

    private static PoiDtos.PoiItem guangzhouTower() {
        return new PoiDtos.PoiItem(
                "B00141V02F", "广州塔", "风景名胜;风景名胜;观景点", "110208",
                "广东省广州市海珠区阅江西路222号",
                LAT, LNG, 4.8, "09:00-22:30", "020-89338222", null);
    }

    @Test
    @DisplayName("高德链接：经度在前（position=113.32,23.10）")
    void amapUsesLongitudeFirst() {
        String url = PoiLinks.amap(LAT, LNG, "广州塔");

        assertTrue(url.contains("position=" + LNG + "," + LAT),
                "高德要求经度在前，实际 URL：" + url);
        assertFalse(url.contains("position=" + LAT + "," + LNG),
                "坐标写反了！这会把用户带到地球另一个地方");
    }

    @Test
    @DisplayName("百度链接：纬度在前（location=23.10,113.32）")
    void baiduUsesLatitudeFirst() {
        String url = PoiLinks.baidu(LAT, LNG, "广州塔", "阅江西路");

        assertTrue(url.contains("location=" + LAT + "," + LNG),
                "百度要求纬度在前，实际 URL：" + url);
        assertFalse(url.contains("location=" + LNG + "," + LAT),
                "坐标写反了！");
    }

    @Test
    @DisplayName("两家的顺序确实是反的")
    void theTwoProvidersAreOpposite() {
        String amap = PoiLinks.amap(LAT, LNG, "x");
        String baidu = PoiLinks.baidu(LAT, LNG, "x", "y");

        String amapCoords = amap.substring(amap.indexOf("position=") + 9, amap.indexOf("&name="));
        String baiduCoords = baidu.substring(baidu.indexOf("location=") + 9, baidu.indexOf("&title="));

        assertEquals(LNG + "," + LAT, amapCoords);
        assertEquals(LAT + "," + LNG, baiduCoords);
        assertFalse(amapCoords.equals(baiduCoords), "两家的坐标顺序必须不一样");
    }

    @Test
    @DisplayName("中文名字会被 URL 编码（不然链接点不开）")
    void chineseIsEncoded() {
        String url = PoiLinks.amap(LAT, LNG, "广州塔");

        assertFalse(url.contains("广州塔"), "中文必须编码，不能直接放进 URL");
        assertTrue(url.contains("%E5%B9%BF%E5%B7%9E%E5%A1%94"), "应该是「广州塔」的 UTF-8 编码");
    }

    @Test
    @DisplayName("美团的名字在路径里：空格要用 %20，不能用 +")
    void meituanPathEncodesSpaceAsPercent20() {
        String url = PoiLinks.meituan("广州 塔");

        assertTrue(url.contains("%20"), "路径段里的空格必须是 %20，实际：" + url);
        assertFalse(url.contains("+"), "路径段里的 + 会被当成字面量的加号，不是空格");
    }

    @Test
    @DisplayName("查询参数里空格用 + 是合法的（跟路径不一样）")
    void queryParamUsesPlusForSpace() {
        String url = PoiLinks.amap(LAT, LNG, "广州 塔");

        assertTrue(url.contains("+") || url.contains("%20"));
        assertFalse(url.contains(" "), "URL 里不能出现裸空格");
    }

    @Test
    @DisplayName("三个链接都生成，而且都是 https")
    void allThreeLinksAreHttps() {
        PoiLinks.Links links = PoiLinks.build(guangzhouTower());

        assertNotNull(links.amap());
        assertNotNull(links.baidu());
        assertNotNull(links.meituan());

        assertTrue(links.amap().startsWith("https://uri.amap.com/"));
        assertTrue(links.baidu().startsWith("https://api.map.baidu.com/"));
        assertTrue(links.meituan().startsWith("https://i.meituan.com/"));
    }

    @Test
    @DisplayName("百度链接必须带 output=html，少了它会返回下载页而不是地图")
    void baiduNeedsOutputHtml() {
        String url = PoiLinks.baidu(LAT, LNG, "广州塔", "阅江西路");
        assertTrue(url.contains("output=html"), "实际：" + url);
    }

    @Test
    @DisplayName("百度链接带上地址，地图上信息更全")
    void baiduCarriesAddress() {
        String url = PoiLinks.baidu(LAT, LNG, "广州塔", "阅江西路222号");
        assertTrue(url.contains("content="));
        assertTrue(url.contains("%E9%98%85%E6%B1%9F"), "地址应该被编码进去");
    }

    @Test
    @DisplayName("POI 为 null → 返回三个 null，不抛异常")
    void nullPoiIsSafe() {
        PoiLinks.Links links = PoiLinks.build(null);
        assertNull(links.amap());
        assertNull(links.baidu());
        assertNull(links.meituan());
    }

    @Test
    @DisplayName("名字为空 → 不崩，链接照样能用（只是没标题）")
    void blankNameIsSafe() {
        PoiLinks.Links links = PoiLinks.build(
                new PoiDtos.PoiItem("id", "", "type", "code", "",
                        23.1, 113.3, null, null, null, null));

        assertTrue(links.amap().contains("position=113.3,23.1"));
        assertNotNull(links.meituan());
    }
}
