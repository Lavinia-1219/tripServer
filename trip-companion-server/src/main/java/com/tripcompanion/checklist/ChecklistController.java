package com.tripcompanion.checklist;

import com.tripcompanion.auth.CurrentUser;
import com.tripcompanion.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/checklist")
public class ChecklistController {

    private final ChecklistService checklistService;
    private final ChecklistSuggestionService suggestionService;

    public ChecklistController(ChecklistService checklistService,
                               ChecklistSuggestionService suggestionService) {
        this.checklistService = checklistService;
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public ApiResponse<List<ChecklistDtos.ItemView>> list(@CurrentUser Long userId,
                                                          @PathVariable Long tripId,
                                                          @RequestParam(required = false) String status) {
        return ApiResponse.ok(checklistService.list(userId, tripId, status));
    }

    @PostMapping
    public ApiResponse<ChecklistDtos.ItemView> create(@CurrentUser Long userId,
                                                      @PathVariable Long tripId,
                                                      @Valid @RequestBody ChecklistDtos.CreateRequest req) {
        return ApiResponse.ok(checklistService.create(userId, tripId, req));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<ChecklistDtos.ItemView> update(@CurrentUser Long userId,
                                                      @PathVariable Long tripId,
                                                      @PathVariable Long itemId,
                                                      @Valid @RequestBody ChecklistDtos.UpdateRequest req) {
        return ApiResponse.ok(checklistService.update(userId, tripId, itemId, req));
    }

    @PatchMapping("/{itemId}/status")
    public ApiResponse<ChecklistDtos.ItemView> changeStatus(@CurrentUser Long userId,
                                                            @PathVariable Long tripId,
                                                            @PathVariable Long itemId,
                                                            @Valid @RequestBody ChecklistDtos.StatusRequest req) {
        return ApiResponse.ok(checklistService.changeStatus(userId, tripId, itemId, req.status()));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> delete(@CurrentUser Long userId,
                                    @PathVariable Long tripId,
                                    @PathVariable Long itemId) {
        checklistService.delete(userId, tripId, itemId);
        return ApiResponse.ok();
    }

    @GetMapping("/suggestions")
    public ApiResponse<ChecklistDtos.SuggestionView> suggestions(@CurrentUser Long userId,
                                                                 @PathVariable Long tripId) {
        return ApiResponse.ok(suggestionService.suggest(userId, tripId));
    }

    @PostMapping("/apply-suggestions")
    public ApiResponse<ChecklistDtos.ApplyResult> applySuggestions(@CurrentUser Long userId,
                                                                   @PathVariable Long tripId) {
        return ApiResponse.ok(suggestionService.applySuggestions(userId, tripId));
    }
}