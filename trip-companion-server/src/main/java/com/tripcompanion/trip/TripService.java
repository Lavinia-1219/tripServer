package com.tripcompanion.trip;

import com.tripcompanion.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TripService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;

    private final TripRepository tripRepository;
    private final TripMemberRepository memberRepository;

    public TripService(TripRepository tripRepository, TripMemberRepository memberRepository) {
        this.tripRepository = tripRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<TripDtos.TripView> listMine(Long userId) {
        List<TripMember> memberships = memberRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Set<Long> tripIds = memberships.stream()
                .map(TripMember::getTripId)
                .collect(Collectors.toSet());

        List<Trip> trips = tripRepository.findByIdInOrderByStartDateDesc(tripIds);

        List<TripMember> allMembers = memberRepository.findAll().stream()
                .filter(m -> tripIds.contains(m.getTripId()))
                .toList();

        return trips.stream()
                .map(trip -> toView(trip, userId, allMembers))
                .sorted(Comparator
                        .comparing((TripDtos.TripView v) -> v.endDate() == null ? 1 : 0)
                        .thenComparing(TripDtos.TripView::startDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripDtos.TripView get(Long userId, Long tripId) {
        Trip trip = requireMember(tripId, userId).trip();
        return toView(trip, userId, memberRepository.findByTripId(tripId));
    }

    @Transactional
    public TripDtos.TripView create(Long userId, TripDtos.CreateRequest req) {
        Trip trip = new Trip(userId, req.title().trim(), generateInviteCode());
        trip.setDestination(trimToNull(req.destination()));
        trip.setCity(trimToNull(req.city()));
        trip.setStartDate(req.startDate());
        trip.setEndDate(req.endDate());
        trip.setType(req.type() == null || req.type().isBlank() ? "city" : req.type().trim());
        tripRepository.save(trip);

        memberRepository.save(new TripMember(trip.getId(), userId, "owner"));

        return toView(trip, userId, memberRepository.findByTripId(trip.getId()));
    }

    @Transactional
    public TripDtos.TripView update(Long userId, Long tripId, TripDtos.UpdateRequest req) {
        MemberContext ctx = requireMember(tripId, userId);
        if (!ctx.member().canEdit()) {
            throw ApiException.forbidden("你没有修改这个行程的权限");
        }

        Trip trip = ctx.trip();
        if (req.title() != null && !req.title().isBlank()) {
            trip.setTitle(req.title().trim());
        }
        if (req.destination() != null) {
            trip.setDestination(trimToNull(req.destination()));
        }
        if (req.city() != null) {
            trip.setCity(trimToNull(req.city()));
        }
        if (req.startDate() != null) {
            trip.setStartDate(req.startDate());
        }
        if (req.endDate() != null) {
            trip.setEndDate(req.endDate());
        }
        if (req.type() != null && !req.type().isBlank()) {
            trip.setType(req.type().trim());
        }

        if (trip.getStartDate() != null && trip.getEndDate() != null
                && trip.getEndDate().isBefore(trip.getStartDate())) {
            throw ApiException.badRequest("返程日期不能早于出发日期");
        }

        trip.touch();
        tripRepository.save(trip);
        return toView(trip, userId, memberRepository.findByTripId(tripId));
    }

    @Transactional
    public void delete(Long userId, Long tripId) {
        MemberContext ctx = requireMember(tripId, userId);
        if (!"owner".equals(ctx.member().getRole())) {
            throw ApiException.forbidden("只有行程创建者可以删除");
        }
        memberRepository.findByTripId(tripId).forEach(memberRepository::delete);
        tripRepository.delete(ctx.trip());
    }

    @Transactional
    public TripDtos.TripView join(Long userId, String inviteCode) {
        String code = inviteCode.trim().toUpperCase();
        Trip trip = tripRepository.findByInviteCode(code)
                .orElseThrow(() -> ApiException.notFound("邀请码无效，或者行程已经被删除了"));

        if (memberRepository.existsByTripIdAndUserId(trip.getId(), userId)) {
            return toView(trip, userId, memberRepository.findByTripId(trip.getId()));
        }

        memberRepository.save(new TripMember(trip.getId(), userId, "editor"));
        return toView(trip, userId, memberRepository.findByTripId(trip.getId()));
    }

    @Transactional(readOnly = true)
    public Trip requireTripForMember(Long tripId, Long userId) {
        return requireMember(tripId, userId).trip();
    }

    @Transactional(readOnly = true)
    public TripMember requireMembership(Long tripId, Long userId) {
        return requireMember(tripId, userId).member();
    }

    private MemberContext requireMember(Long tripId, Long userId) {
        TripMember member = memberRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> ApiException.notFound("行程不存在，或者你还没有加入"));
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("行程不存在，或者你还没有加入"));
        return new MemberContext(trip, member);
    }

    private record MemberContext(Trip trip, TripMember member) {
    }

    private TripDtos.TripView toView(Trip trip, Long userId, List<TripMember> members) {
        String role = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .map(TripMember::getRole)
                .findFirst()
                .orElse("viewer");

        return new TripDtos.TripView(
                trip.getId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getType(),
                trip.getInviteCode(),
                role,
                members.size(),
                "owner".equals(role),
                trip.getUpdatedAt()
        );
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
            }
            String code = sb.toString();
            if (tripRepository.findByInviteCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("生成邀请码连续冲突，请检查随机数实现");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
