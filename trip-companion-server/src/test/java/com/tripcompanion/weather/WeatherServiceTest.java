package com.tripcompanion.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherServiceTest {

    private static final class FakeClient implements WeatherClient {

        private final String name;
        private final boolean configured;
        private final boolean failing;
        private final WeatherDtos.DailyWeather day;
        private int calls = 0;

        FakeClient(String name, boolean configured, boolean failing, WeatherDtos.DailyWeather day) {
            this.name = name;
            this.configured = configured;
            this.failing = failing;
            this.day = day;
        }

        @Override
        public String provider() {
            return name;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public List<WeatherDtos.DailyWeather> fetchDaily(String city, int days) {
            calls++;
            if (failing) {
                throw new IllegalStateException(name + " 网络超时");
            }
            return List.of(day);
        }
    }

    private static WeatherDtos.DailyWeather day(String text, int tempMin, int tempMax) {
        return new WeatherDtos.DailyWeather(
                LocalDate.of(2026, 9, 15), text, text, tempMin, tempMax, 60, "东北风", "1-3", "fake");
    }

    private static WeatherService service(WeatherClient... clients) {
        return new WeatherService(List.of(clients), null, "qweather,amap", 60);
    }

    @Test
    @DisplayName("第一个数据源成功 → 直接用它")
    void firstProviderWins() {
        FakeClient qweather = new FakeClient("qweather", true, false, day("晴", 20, 30));
        FakeClient amap = new FakeClient("amap", true, false, day("多云", 18, 28));

        WeatherDtos.WeatherView view = service(qweather, amap).getWeather("广州", 3);

        assertThat(view.available()).isTrue();
        assertThat(view.provider()).isEqualTo("qweather");
        assertThat(amap.calls).isZero();
    }

    @Test
    @DisplayName("第一个数据源失败 → 自动切到第二个（这就是「降级切换」）")
    void fallsBackToSecondProvider() {
        FakeClient qweather = new FakeClient("qweather", true, true, null);
        FakeClient amap = new FakeClient("amap", true, false, day("多云", 18, 28));

        WeatherDtos.WeatherView view = service(qweather, amap).getWeather("广州", 3);

        assertThat(view.available()).isTrue();
        assertThat(view.provider()).isEqualTo("amap");
        assertThat(qweather.calls).isEqualTo(1);
        assertThat(amap.calls).isEqualTo(1);
    }

    @Test
    @DisplayName("未配置密钥的数据源会被直接跳过，不浪费一次网络请求")
    void skipsUnconfiguredProvider() {
        FakeClient qweather = new FakeClient("qweather", false, false, day("晴", 20, 30));
        FakeClient amap = new FakeClient("amap", true, false, day("多云", 18, 28));

        WeatherDtos.WeatherView view = service(qweather, amap).getWeather("广州", 3);

        assertThat(view.provider()).isEqualTo("amap");
        assertThat(qweather.calls)
                .as("密钥都没配，就不该去请求")
                .isZero();
    }

    @Test
    @DisplayName("所有数据源都失败 → 降级返回，不是抛异常")
    void allProvidersFail() {
        FakeClient qweather = new FakeClient("qweather", true, true, null);
        FakeClient amap = new FakeClient("amap", true, true, null);

        WeatherDtos.WeatherView view = service(qweather, amap).getWeather("广州", 3);

        assertThat(view.available()).isFalse();
        assertThat(view.message()).contains("qweather").contains("amap");
        assertThat(view.days()).isEmpty();
    }

    @Test
    @DisplayName("一个密钥都没配 → 提示写清楚该配什么")
    void nothingConfigured() {
        FakeClient qweather = new FakeClient("qweather", false, false, day("晴", 20, 30));
        FakeClient amap = new FakeClient("amap", false, false, day("晴", 20, 30));

        WeatherDtos.WeatherView view = service(qweather, amap).getWeather("广州", 3);

        assertThat(view.available()).isFalse();
        assertThat(view.message()).contains("没有配置");
    }

    @Test
    @DisplayName("缓存生效：同一城市连查两次，外部只被调用一次")
    void cacheHitsOnSecondCall() {
        FakeClient qweather = new FakeClient("qweather", true, false, day("晴", 20, 30));
        WeatherService svc = service(qweather);

        WeatherDtos.WeatherView first = svc.getWeather("广州", 3);
        WeatherDtos.WeatherView second = svc.getWeather("广州", 3);

        assertThat(first.fromCache()).isFalse();
        assertThat(second.fromCache())
                .as("第二次应该命中缓存")
                .isTrue();
        assertThat(qweather.calls)
                .as("外部数据源只该被调用一次")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("不同城市 / 不同天数各用一份缓存，不会串")
    void cacheKeyIncludesCityAndDays() {
        FakeClient qweather = new FakeClient("qweather", true, false, day("晴", 20, 30));
        WeatherService svc = service(qweather);

        svc.getWeather("广州", 3);
        svc.getWeather("上海", 3);
        svc.getWeather("广州", 5);

        assertThat(qweather.calls)
                .as("三次都是不同的 key，都该真的去请求")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("降级结果不进缓存 —— 否则网络抖一下，半小时内所有人都看不到天气")
    void degradedResultIsNotCached() {
        FakeClient qweather = new FakeClient("qweather", true, true, null);
        WeatherService svc = service(qweather);

        svc.getWeather("广州", 3);
        svc.getWeather("广州", 3);

        assertThat(qweather.calls)
                .as("降级结果不缓存，所以两次都会真的去试")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("下雨 → 建议带伞")
    void rainyNeedsUmbrella() {
        FakeClient qweather = new FakeClient("qweather", true, false, day("小雨", 20, 25));

        WeatherDtos.WeatherView view = service(qweather).getWeather("广州", 1);

        assertThat(view.advice().needUmbrella()).isTrue();
        assertThat(view.advice().tips()).anyMatch(tip -> tip.contains("伞"));
    }

    @Test
    @DisplayName("30 度以上 → 建议短袖，并提醒防晒")
    void hotDayAdvice() {
        FakeClient qweather = new FakeClient("qweather", true, false, day("晴", 26, 33));

        WeatherDtos.WeatherView view = service(qweather).getWeather("广州", 1);

        assertThat(view.advice().clothing()).contains("短袖");
        assertThat(view.advice().needSunscreen()).isTrue();
    }

    @Test
    @DisplayName("0 度以下 → 建议羽绒服")
    void freezingAdvice() {
        FakeClient qweather = new FakeClient("qweather", true, false, day("晴", -8, -1));

        WeatherDtos.WeatherView view = service(qweather).getWeather("哈尔滨", 1);

        assertThat(view.advice().clothing()).contains("羽绒服");
        assertThat(view.advice().needUmbrella()).isFalse();
    }

    @Test
    @DisplayName("早晚温差大 → 提醒带外套")
    void bigTemperatureSwing() {
        FakeClient qweather = new FakeClient("qweather", true, false, day("晴", 10, 25));

        WeatherDtos.WeatherView view = service(qweather).getWeather("昆明", 1);

        assertThat(view.advice().tips()).anyMatch(tip -> tip.contains("温差"));
    }

    @Test
    @DisplayName("温度数据缺失时不崩，给出保守建议")
    void missingTemperatureIsSafe() {
        WeatherDtos.DailyWeather noTemp = new WeatherDtos.DailyWeather(
                LocalDate.of(2026, 9, 15), "晴", "晴", null, null, null, null, null, "fake");
        FakeClient qweather = new FakeClient("qweather", true, false, noTemp);

        WeatherDtos.WeatherView view = service(qweather).getWeather("广州", 1);

        assertThat(view.available()).isTrue();
        assertThat(view.advice().clothing()).isNotNull();
    }
}
