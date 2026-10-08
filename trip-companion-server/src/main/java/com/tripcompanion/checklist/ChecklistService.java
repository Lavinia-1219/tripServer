package com.tripcompanion.checklist;

import com.tripcompanion.common.ApiException;
import com.tripcompanion.trip.TripMemberRepository;
import com.tripcompanion.trip.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChecklistService {

    private static final List<String> ALLOWED_STATUS =
            List.of(ChecklistItem.STATUS_PENDING, ChecklistItem.STATUS_PACKED);

    private final ChecklistItemRepository itemRepository;
    private final TripService tripService;
    private final TripMemberRepository memberRepository;

    public ChecklistService(ChecklistItemRepository itemRepository,
                            TripService tripService,
                            TripMemberRepository memberRepository) {
        this.itemRepository = itemRepository;
        this.tripService = tripService;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<ChecklistDtos.ItemView> list(Long userId, Long tripId, String status) {
        tripService.requireTripForMember(tripId, userId);

        List<ChecklistItem> items;
        if (status == null || status.isBlank()) {
            items = itemRepository.findByTripIdOrderByIdAsc(tripId);
        } else {
            items = itemRepository.findByTripIdAndStatusOrderByIdAsc(tripId, status);
        }
        return items.stream().map(this::toView).toList();
    }

    @Transactional
    public ChecklistDtos.ItemView create(Long userId, Long tripId, ChecklistDtos.CreateRequest req) {
        tripService.requireTripForMember(tripId, userId);
        requireAssigneeIsMember(tripId, req.assigneeId());

        ChecklistItem item = new ChecklistItem(tripId, req.name());
        item.setCategory(req.category());
        if (req.quantity() != null) {
            item.setQuantity(req.quantity());
        }
        item.setAssigneeId(req.assigneeId());

        return toView(itemRepository.save(item));
    }

    @Transactional
    public ChecklistDtos.ItemView update(Long userId, Long tripId, Long itemId,
                                         ChecklistDtos.UpdateRequest req) {
        tripService.requireTripForMember(tripId, userId);
        requireAssigneeIsMember(tripId, req.assigneeId());

        ChecklistItem item = loadItem(tripId, itemId);

        if (req.name() != null) {
            item.setName(req.name());
        }
        if (req.category() != null) {
            item.setCategory(req.category());
        }
        if (req.quantity() != null) {
            item.setQuantity(req.quantity());
        }
        if (req.assigneeId() != null) {
            item.setAssigneeId(req.assigneeId());
        }
        item.touch();

        return toView(itemRepository.save(item));
    }

    @Transactional
    public ChecklistDtos.ItemView changeStatus(Long userId, Long tripId, Long itemId, String status) {
        tripService.requireTripForMember(tripId, userId);

        if (status == null || !ALLOWED_STATUS.contains(status)) {
            throw ApiException.badRequest("状态只能是 pending 或 packed");
        }

        ChecklistItem item = loadItem(tripId, itemId);
        item.setStatus(status);
        item.touch();

        return toView(itemRepository.save(item));
    }

    @Transactional
    public void delete(Long userId, Long tripId, Long itemId) {
        tripService.requireTripForMember(tripId, userId);
        itemRepository.delete(loadItem(tripId, itemId));
    }

    private ChecklistItem loadItem(Long tripId, Long itemId) {
        return itemRepository.findByIdAndTripId(itemId, tripId)
                .orElseThrow(() -> ApiException.notFound("清单项不存在"));
    }

    private void requireAssigneeIsMember(Long tripId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        if (!memberRepository.existsByTripIdAndUserId(tripId, assigneeId)) {
            throw ApiException.badRequest("指定的负责人不在这个行程里");
        }
    }

    private ChecklistDtos.ItemView toView(ChecklistItem item) {
        return new ChecklistDtos.ItemView(
                item.getId(),
                item.getTripId(),
                item.getName(),
                item.getCategory(),
                item.getQuantity(),
                item.getAssigneeId(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}