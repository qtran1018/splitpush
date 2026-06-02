package com.splitpush.repository;

import com.splitpush.model.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteTokenRepository extends JpaRepository<InviteToken, String> {
    @Query("SELECT i FROM InviteToken i LEFT JOIN FETCH i.tripGroup g LEFT JOIN FETCH i.createdBy WHERE i.token = :token")
    Optional<InviteToken> findByTokenWithDetails(@Param("token") String token);
}
