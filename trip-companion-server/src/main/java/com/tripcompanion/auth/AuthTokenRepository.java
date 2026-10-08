package com.tripcompanion.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {

    @Modifying
    @Query("delete from AuthToken t where t.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
