package com.tripcompanion.checklist;

import com.tripcompanion.trip.Trip;
import com.tripcompanion.trip.TripService;
import com.tripcompanion.weather.WeatherDtos;
import com.tripcompanion.weather.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChecklistSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(ChecklistSuggestionService.class);

    private final TripService tripService;
    private final WeatherService weatherService;
    private final ChecklistItemRepository itemRepository;
    private final ChecklistRuleRepository ruleRepository;

    public ChecklistSuggestionService(TripService tripService,
                                      WeatherService weatherService,
                                      ChecklistItemRepository itemRepository,
                                      ChecklistRuleRepository ruleRepository) {
        this.tripService = tripService;
        this.weatherService = weatherService;
        this.itemRepository = itemRepository;
        this.ruleRepository = ruleRepository;
    }

    public ChecklistDtos.SuggestionView suggest(Long userId, Long tripId) {
        Trip trip = tripService.requireTripForMember(tripId, userId);

        int days = forecastDays(trip);

        WeatherDtos.WeatherView weather = weatherService.getWeatherForTrip(userId, tripId, days);

        if (!weather.available()) {
            log.debug("行程 {} 的天气不可用，返回空建议：{}", tripId, weather.message());
            return new ChecklistDtos.SuggestionView(
                    tripId, weather.city(), false, weather.message(), days, List.of());
        }

        List<String> existingNames = itemRepository.findByTripIdOrderByIdAsc(tripId)
                .stream()
                .map(ChecklistItem::getName)
                .toList();

        List<ChecklistRule> rules = ruleRepository.findByEnabledTrueOrderByPriorityAscIdAsc();

        List<RuleEngine.Hit> hits = RuleEngine.evaluate(weather.days(), rules, existingNames);

        List<ChecklistDtos.Suggestion> suggestions = hits.stream()
                .map(h -> new ChecklistDtos.Suggestion(
                        h.itemName(), h.category(), h.reason(), h.dates()))
                .toList();

        return new ChecklistDtos.SuggestionView(
                tripId,
                weather.city(),
                true,
                forecastCaveat(trip, days, weather.message()),
                days,
                suggestions);
    }

    @Transactional
    public ChecklistDtos.ApplyResult applySuggestions(Long userId, Long tripId) {
        ChecklistDtos.SuggestionView view = suggest(userId, tripId);

        List<ChecklistDtos.ItemView> added = new ArrayList<>();
        for (ChecklistDtos.Suggestion suggestion : view.suggestions()) {
            ChecklistItem item = new ChecklistItem(tripId, suggestion.name());
            item.setCategory(suggestion.category());

            ChecklistItem saved = itemRepository.save(item);
            added.add(new ChecklistDtos.ItemView(
                    saved.getId(),
                    saved.getTripId(),
                    saved.getName(),
                    saved.getCategory(),
                    saved.getQuantity(),
                    saved.getAssigneeId(),
                    saved.getStatus(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt()));
        }

        log.info("行程 {} 采纳建议：{} 条，实际新增 {} 条", tripId, view.suggestions().size(), added.size());
        return new ChecklistDtos.ApplyResult(view.suggestions().size(), added.size(), List.copyOf(added));
    }

    private static int forecastDays(Trip trip) {
        LocalDate today = LocalDate.now();
        LocalDate end = trip.getEndDate();

        if (end == null) {
            return WeatherService.DEFAULT_DAYS;
        }

        long need = ChronoUnit.DAYS.between(today, end) + 1;
        if (need < 1) {
            need = 1;
        }
        return (int) Math.min(need, WeatherService.MAX_DAYS);
    }

    private static String forecastCaveat(Trip trip, int days, String existingMessage) {
        if (existingMessage != null && !existingMessage.isBlank()) {
            return existingMessage;
        }
        LocalDate start = trip.getStartDate();
        if (start == null) {
            return null;
        }
        LocalDate lastForecastDate = LocalDate.now().plusDays(days - 1L);
        if (start.isAfter(lastForecastDate)) {
            return "行程还没进入 " + days + " 天预报范围，下面按最近的天气给个参考，出发前记得再看一次";
        }
        return null;
    }
}
