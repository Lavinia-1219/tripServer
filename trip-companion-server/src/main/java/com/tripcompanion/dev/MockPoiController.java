package com.tripcompanion.dev;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/poi")
public class MockPoiController {

    private final MockPoiClient mockClient;

    public MockPoiController(MockPoiClient mockClient) {
        this.mockClient = mockClient;
    }

    @PostMapping("/mock")
    public ApiResponse<Map<String, Object>> enable(@CurrentUser Long userId) {
        mockClient.setEnabled(true);
        return ApiResponse.ok(state("已启用假 POI 数据"));
    }

    @DeleteMapping("/mock")
    public ApiResponse<Map<String, Object>> disable(@CurrentUser Long userId) {
        mockClient.setEnabled(false);
        return ApiResponse.ok(state("已关闭假 POI 数据，回到真实数据源"));
    }

    @GetMapping("/mock")
    public ApiResponse<Map<String, Object>> current(@CurrentUser Long userId) {
        return ApiResponse.ok(state("当前假 POI 状态"));
    }

    private Map<String, Object> state(String note) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("note", note);
        body.put("enabled", mockClient.isEnabled());
        body.put("provider", mockClient.provider());
        body.put("hint", "假数据固定在广州塔附近，跑验收时请用「广州」的行程");
        return body;
    }
}
