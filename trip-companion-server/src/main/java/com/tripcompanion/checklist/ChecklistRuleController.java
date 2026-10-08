package com.tripcompanion.checklist;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checklist/rules")
public class ChecklistRuleController {

    private final ChecklistRuleService ruleService;

    public ChecklistRuleController(ChecklistRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@CurrentUser Long userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("rules", ruleService.list());
        body.put("supportedFields", RuleEngine.supportedFields().stream().sorted().toList());
        body.put("supportedOperators", RuleEngine.supportedOperators().stream().sorted().toList());
        return ApiResponse.ok(body);
    }

    @PostMapping
    public ApiResponse<ChecklistDtos.RuleView> create(@CurrentUser Long userId,
                                                      @Valid @RequestBody ChecklistDtos.RuleRequest req) {
        return ApiResponse.ok(ruleService.create(req));
    }

    @PatchMapping("/{ruleId}/enabled")
    public ApiResponse<ChecklistDtos.RuleView> setEnabled(@CurrentUser Long userId,
                                                          @PathVariable Long ruleId,
                                                          @RequestParam boolean value) {
        return ApiResponse.ok(ruleService.setEnabled(ruleId, value));
    }
}
