package com.tripcompanion.poi;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PoiController {

    private static final String DEFAULT_CATEGORY = "attraction";

    private final PoiService poiService;

    public PoiController(PoiService poiService) {
        this.poiService = poiService;
    }

    @GetMapping("/trips/{tripId}/poi")
    public ApiResponse<PoiDtos.RecommendView> recommend(@CurrentUser Long userId,
                                                        @PathVariable Long tripId,
                                                        @RequestParam(required = false) String category,
                                                        @RequestParam(required = false) Integer limit,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Double lat,
                                                        @RequestParam(required = false) Double lng,
                                                        @RequestParam(required = false) Integer radius) {
        String cat = (category == null || category.isBlank()) ? DEFAULT_CATEGORY : category;
        return ApiResponse.ok(
                poiService.recommend(userId, tripId, cat, limit, keyword, lat, lng, radius));
    }

    @GetMapping("/poi/preferences")
    public ApiResponse<PoiDtos.PreferenceOptions> preferences(@CurrentUser Long userId) {
        return ApiResponse.ok(poiService.preferenceOptions());
    }
}
