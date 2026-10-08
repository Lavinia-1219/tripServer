package com.tripcompanion.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByInviteCode(String inviteCode);

    List<Trip> findByIdInOrderByStartDateDesc(Collection<Long> ids);
}
