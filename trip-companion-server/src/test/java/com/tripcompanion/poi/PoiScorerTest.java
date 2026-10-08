package com.tripcompanion.poi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiScorerTest {

    private static final PoiDtos.GeoPoint TOWER = new PoiDtos.GeoPoint(23.1065, 113.3245);

    private static PoiDtos.PoiItem poi(String name, double dLat, double dLng,
                                       Double rating, String type) {
        return new PoiDtos.PoiItem(
                "id-" + name, name, type, "999999", "地址",
                23.1065 + dLat, 113.3245 + dLng,
                rating, null, null, null);
    }

    private static PoiScorer.Weights w(double pref, double dist, double rate) {
        return new PoiScorer.Weights(pref, dist, rate);
    }

    private static PoiScorer.Weights defaultWeights() {
        return new PoiScorer.Weights(0.5, 0.3, 0.2);
    }

    @Test
    @DisplayName("同一个点 → 距离 0")
    void samePoint_zeroDistance() {
        assertEquals(0.0, PoiScorer.distanceMeters(23.1065, 113.3245, 23.1065, 113.3245), 0.001);
    }

    @Test
    @DisplayName("纬度差 0.001 度 ≈ 111 米")
    void oneThousandthDegree_isAbout111m() {
        double d = PoiScorer.distanceMeters(23.1065, 113.3245, 23.1075, 113.3245);
        assertEquals(111.19, d, 2.0);
    }

    @Test
    @DisplayName("经度差同样度数，在高纬度处距离更短")
    void longitudeShrinksWithLatitude() {
        double nearEquator = PoiScorer.distanceMeters(0.0, 113.0, 0.0, 113.001);
        double inGuangzhou = PoiScorer.distanceMeters(23.1, 113.0, 23.1, 113.001);
        assertTrue(inGuangzhou < nearEquator,
                "北纬 23 度处的 0.001 经度应该比赤道处短，实际 " + inGuangzhou + " vs " + nearEquator);
    }

    @Test
    @DisplayName("对称性：A→B 和 B→A 一样远")
    void distanceIsSymmetric() {
        double ab = PoiScorer.distanceMeters(23.10, 113.32, 23.15, 113.40);
        double ba = PoiScorer.distanceMeters(23.15, 113.40, 23.10, 113.32);
        assertEquals(ab, ba, 0.001);
    }

    @Test
    @DisplayName("评分分：5 分 → 1.0，3 分 → 0.0，4 分 → 0.5")
    void ratingScore_linear() {
        assertEquals(1.0, PoiScorer.ratingScore(5.0), 0.001);
        assertEquals(0.0, PoiScorer.ratingScore(3.0), 0.001);
        assertEquals(0.5, PoiScorer.ratingScore(4.0), 0.001);
    }

    @Test
    @DisplayName("评分分：超出范围会被夹住，不会算出负数或大于 1")
    void ratingScore_clamped() {
        assertEquals(0.0, PoiScorer.ratingScore(1.0), 0.001);
        assertEquals(1.0, PoiScorer.ratingScore(9.9), 0.001);
    }

    @Test
    @DisplayName("没评分 → 中立 0.5，不能当成 0 分")
    void ratingScore_null_returnsNeutral() {
        assertEquals(0.5, PoiScorer.ratingScore(null), 0.001,
                "没评分的店不该被当成 0 分惩罚 —— 它只是数据缺失");
    }

    @Test
    @DisplayName("偏好「自然风光」+ POI 是公园 → 命中 1.0")
    void preferenceHit() {
        PoiDtos.PoiItem park = poi("某某公园", 0.001, 0.001, 4.5, "风景名胜;公园广场;公园");
        assertEquals(1.0, PoiScorer.preferenceScore(park, List.of("自然风光")), 0.001);
    }

    @Test
    @DisplayName("偏好「自然风光」+ POI 是酒吧 → 不命中 0.0")
    void preferenceMiss() {
        PoiDtos.PoiItem bar = poi("某某酒吧", 0.001, 0.001, 4.5, "娱乐场所;酒吧");
        assertEquals(0.0, PoiScorer.preferenceScore(bar, List.of("自然风光")), 0.001);
    }

    @Test
    @DisplayName("没设偏好 → 中立 0.5")
    void noPreference_neutral() {
        PoiDtos.PoiItem park = poi("某某公园", 0.001, 0.001, 4.5, "风景名胜;公园广场;公园");
        assertEquals(0.5, PoiScorer.preferenceScore(park, List.of()), 0.001);
        assertEquals(0.5, PoiScorer.preferenceScore(park, null), 0.001);
    }

    @Test
    @DisplayName("名字里命中关键词也算（不只匹配 type）")
    void preferenceMatchesNameToo() {
        PoiDtos.PoiItem m = poi("城市博物馆", 0.001, 0.001, 4.5, "科教文化服务;文化宫");
        assertEquals(1.0, PoiScorer.preferenceScore(m, List.of("历史人文")), 0.001);
    }

    @Test
    @DisplayName("设了偏好但全批都没命中 → 偏好分全体归中立，不把距离评分压扁")
    void noHitInBatch_preferenceBecomesNeutral() {
        List<PoiDtos.PoiItem> foods = List.of(
                poi("快餐A", 0.005, 0.005, 3.0, "餐饮服务;快餐厅"),
                poi("快餐B", 0.005, 0.005, 5.0, "餐饮服务;快餐厅"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(foods, TOWER,
                List.of("自然风光"), defaultWeights());

        for (PoiDtos.ScoredPoi sp : out) {
            assertEquals(0.5, sp.preferenceScore(), 0.001,
                    "全批一个都没命中时，偏好分应该归中立，而不是全体 0");
        }
        assertEquals("快餐B", out.get(0).poi().name(),
                "偏好不起作用时，评分高的应该赢；实际顺序：" + names(out));
    }

    @Test
    @DisplayName("距离分按「距离基准」算：越近越高，超出基准记 0")
    void distanceScoreIsRelativeToRadius() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("很近", 0.001, 0.001, 4.0, "风景名胜;公园广场;公园"),
                poi("中等", 0.010, 0.010, 4.0, "风景名胜;公园广场;公园"),
                poi("超远", 0.200, 0.200, 4.0, "风景名胜;公园广场;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());

        PoiDtos.ScoredPoi near = out.stream().filter(s -> s.poi().name().equals("很近")).findFirst().orElseThrow();
        PoiDtos.ScoredPoi mid = out.stream().filter(s -> s.poi().name().equals("中等")).findFirst().orElseThrow();
        PoiDtos.ScoredPoi far = out.stream().filter(s -> s.poi().name().equals("超远")).findFirst().orElseThrow();

        assertTrue(near.distanceScore() > mid.distanceScore(),
                "越近分越高：" + near.distanceScore() + " vs " + mid.distanceScore());
        assertTrue(mid.distanceScore() > far.distanceScore(),
                "越近分越高：" + mid.distanceScore() + " vs " + far.distanceScore());

        assertTrue(near.distanceScore() > 0.95,
                "150 米应该接近满分，实际 " + near.distanceScore());
        assertEquals(0.0, far.distanceScore(), 0.001, "超出 10km 基准 → 距离分记 0");
    }

    @Test
    @DisplayName("全都在基准之外 → 退化成相对归一化，至少能区分「谁更近」")
    void allBeyondRadius_fallsBackToRelative() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("30公里", 0.270, 0.0, 4.0, "风景名胜;公园"),
                poi("50公里", 0.450, 0.0, 4.0, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());

        PoiDtos.ScoredPoi nearer = out.stream().filter(s -> s.poi().name().equals("30公里")).findFirst().orElseThrow();
        PoiDtos.ScoredPoi farther = out.stream().filter(s -> s.poi().name().equals("50公里")).findFirst().orElseThrow();

        assertEquals(1.0, nearer.distanceScore(), 0.001, "退化成相对归一化后，最近的应该是 1.0");
        assertEquals(0.0, farther.distanceScore(), 0.001, "最远的应该是 0.0");
    }

    @Test
    @DisplayName("所有 POI 挤在同一个点 → 距离分全体 1.0，不会除以 0")
    void allSamePoint_noDivideByZero() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("甲", 0.0, 0.0, 4.0, "风景名胜;公园广场;公园"),
                poi("乙", 0.0, 0.0, 4.0, "风景名胜;公园广场;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());

        for (PoiDtos.ScoredPoi sp : out) {
            assertEquals(1.0, sp.distanceScore(), 0.001);
        }
    }

    @Test
    @DisplayName("没有参考点 → 距离分中立 0.5，不报错")
    void nullReference_neutralDistance() {
        List<PoiDtos.PoiItem> pois = List.of(poi("甲", 0.001, 0.001, 4.0, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, null, List.of(), defaultWeights());

        assertEquals(0.5, out.get(0).distanceScore(), 0.001);
    }

    @Test
    @DisplayName("改权重 → 排序跟着变（近但评分低 vs 远但评分高）")
    void changingWeights_changesOrder() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("近但评分低", 0.002, 0.002, 3.1, "风景名胜;公园"),
                poi("远但评分高", 0.030, 0.030, 5.0, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> byDistance = PoiScorer.score(pois, TOWER, List.of(),
                w(0.0, 1.0, 0.0));
        assertEquals("近但评分低", byDistance.get(0).poi().name(),
                "距离权重拉满时，最近的应该排第一，实际：" + names(byDistance));

        List<PoiDtos.ScoredPoi> byRating = PoiScorer.score(pois, TOWER, List.of(),
                w(0.0, 0.0, 1.0));
        assertEquals("远但评分高", byRating.get(0).poi().name(),
                "评分权重拉满时，5 分的应该排第一，实际：" + names(byRating));
    }

    @Test
    @DisplayName("权重会自动归一化：5,3,2 等价于 0.5,0.3,0.2")
    void weightsAreNormalized() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("甲", 0.002, 0.002, 4.8, "风景名胜;公园"),
                poi("乙", 0.030, 0.030, 3.5, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> a = PoiScorer.score(pois, TOWER, List.of(), w(0.5, 0.3, 0.2));
        List<PoiDtos.ScoredPoi> b = PoiScorer.score(pois, TOWER, List.of(), w(5.0, 3.0, 2.0));

        assertEquals(a.get(0).score(), b.get(0).score(), 0.0001,
                "权重整体放大不该改变结果");
    }

    @Test
    @DisplayName("权重不合法（负数/全零）会被兜住，不会算出 NaN")
    void invalidWeights_fallbackToDefault() {
        List<PoiDtos.PoiItem> pois = List.of(poi("甲", 0.002, 0.002, 4.5, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), w(0, 0, 0));

        double score = out.get(0).score();
        assertFalse(Double.isNaN(score), "总分不能是 NaN");
        assertTrue(score >= 0 && score <= 1, "总分应该落在 [0,1]，实际 " + score);
    }

    @Test
    @DisplayName("分数完全相同时，按距离升序 → 再按名字升序（顺序稳定）")
    void sortTieBreakersAreStable() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("3号店", 0.005, 0.005, 4.0, "风景名胜;公园"),
                poi("1号店", 0.005, 0.005, 4.0, "风景名胜;公园"),
                poi("2号店", 0.005, 0.005, 4.0, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());

        assertEquals(List.of("1号店", "2号店", "3号店"), names(out),
                "同分时应该按名字排 —— 保证同一份数据每次调用顺序一致");
    }

    @Test
    @DisplayName("（补）中文名字是按 Unicode 码点排，不是拼音 —— 跟直觉相反")
    void chineseNameSortsByCodePoint() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("甲", 0.005, 0.005, 4.0, "风景名胜;公园"),
                poi("乙", 0.005, 0.005, 4.0, "风景名胜;公园"),
                poi("丙", 0.005, 0.005, 4.0, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());

        assertEquals(List.of("丙", "乙", "甲"), names(out));
    }

    @Test
    @DisplayName("近的排前面（同评分同偏好时）")
    void nearerWins() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("远", 0.010, 0.010, 4.5, "风景名胜;公园"),
                poi("近", 0.001, 0.001, 4.5, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());

        assertEquals("近", out.get(0).poi().name());
    }

    @Test
    @DisplayName("关键字搜索：名字命中的排第一，不被「更近的那个」压过")
    void keywordSearch_putsNameMatchFirst() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("越秀公园", 0.010, 0.010, 4.8, "风景名胜;公园广场;公园"),
                poi("广州塔", 0.060, 0.060, 4.8, "风景名胜;观景点"));

        List<PoiDtos.ScoredPoi> plain = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());
        assertEquals("越秀公园", plain.get(0).poi().name(),
                "不搜关键字时，应该按距离+评分排");

        List<PoiDtos.ScoredPoi> byKeyword = PoiScorer.score(
                pois, TOWER, List.of(), "广州塔", defaultWeights());
        assertEquals("广州塔", byKeyword.get(0).poi().name(),
                "用户明确搜了名字，它就该排第一；实际：" + names(byKeyword));
    }

    @Test
    @DisplayName("关键字匹配：只看名字，不看 type")
    void keywordMatchLooksAtNameOnly() {
        PoiDtos.PoiItem other = poi("白云山", 0.01, 0.01, 4.5, "风景名胜;观景点");
        assertEquals(0.0, PoiScorer.keywordMatchScore(other, "广州塔"), 0.001);

        PoiDtos.PoiItem tower = poi("广州塔", 0.01, 0.01, 4.5, "风景名胜;观景点");
        assertEquals(1.0, PoiScorer.keywordMatchScore(tower, "广州塔"), 0.001);
    }

    @Test
    @DisplayName("关键字一个都没命中名字 → 退化成中立，不把整个排序锁死")
    void keywordNoHit_fallsBackToNeutral() {
        List<PoiDtos.PoiItem> pois = List.of(
                poi("甲公园", 0.005, 0.005, 3.0, "风景名胜;公园"),
                poi("乙公园", 0.005, 0.005, 5.0, "风景名胜;公园"));

        List<PoiDtos.ScoredPoi> out = PoiScorer.score(
                pois, TOWER, List.of(), "不存在的名字", defaultWeights());

        for (PoiDtos.ScoredPoi sp : out) {
            assertEquals(0.5, sp.preferenceScore(), 0.001, "全没命中时该维度应该中立");
        }
        assertEquals("乙公园", out.get(0).poi().name(), "偏好维度中立后，评分高的赢");
    }

    @Test
    @DisplayName("空列表 → 空结果，不报错")
    void emptyInput() {
        assertEquals(List.of(), PoiScorer.score(List.of(), TOWER, List.of(), defaultWeights()));
        assertEquals(List.of(), PoiScorer.score(null, TOWER, List.of(), defaultWeights()));
    }

    @Test
    @DisplayName("权重传 null → 用默认值，不报错")
    void nullWeights_useDefault() {
        List<PoiDtos.PoiItem> pois = List.of(poi("甲", 0.001, 0.001, 4.5, "风景名胜;公园"));
        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), null);
        assertEquals(1, out.size());
    }

    @Test
    @DisplayName("每条结果都带上了跳转链接")
    void everyResultHasLinks() {
        List<PoiDtos.PoiItem> pois = List.of(poi("甲", 0.001, 0.001, 4.5, "风景名胜;公园"));
        List<PoiDtos.ScoredPoi> out = PoiScorer.score(pois, TOWER, List.of(), defaultWeights());

        PoiLinks.Links links = out.get(0).links();
        assertTrue(links.amap().startsWith("https://uri.amap.com/marker"), links.amap());
        assertTrue(links.baidu().startsWith("https://api.map.baidu.com/marker"), links.baidu());
        assertTrue(links.meituan().startsWith("https://i.meituan.com/"), links.meituan());
    }

    @Test
    @DisplayName("偏好字符串：去空格、去重、丢掉不认识的标签")
    void parsePreferences_cleans() {
        assertEquals(List.of("自然风光", "美食"),
                PoiScorer.parsePreferences("自然风光, 美食"));
        assertEquals(List.of("美食"),
                PoiScorer.parsePreferences("美食,美食,美食"));
        assertEquals(List.of("美食"),
                PoiScorer.parsePreferences("美食,这个标签根本不存在"),
                "不认识的标签应该被丢掉，而不是原样留着");
        assertEquals(List.of(), PoiScorer.parsePreferences(""));
        assertEquals(List.of(), PoiScorer.parsePreferences(null));
        assertEquals(List.of(), PoiScorer.parsePreferences("  ,  , "));
    }

    @Test
    @DisplayName("可选偏好标签不为空，而且每个都有对应关键词")
    void preferenceOptionsAreWellFormed() {
        List<String> all = PoiScorer.allPreferences();
        assertTrue(all.size() >= 5, "偏好标签太少：" + all);
        for (String tag : all) {
            assertTrue(PoiScorer.preferenceMapping().containsKey(tag), "标签 " + tag + " 没有对应关键词");
            assertTrue(!PoiScorer.preferenceMapping().get(tag).isEmpty(), "标签 " + tag + " 的关键词是空的");
        }
    }

    private static List<String> names(List<PoiDtos.ScoredPoi> list) {
        return list.stream().map(s -> s.poi().name()).toList();
    }
}
