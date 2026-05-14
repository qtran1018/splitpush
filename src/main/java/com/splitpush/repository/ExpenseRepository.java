package com.splitpush.repository;

import com.splitpush.model.Expense;
import com.splitpush.model.TripGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByTripGroup(TripGroup tripGroup);
    List<Expense> findByTripGroupOrderByCreatedAtDesc(TripGroup tripGroup);
    Page<Expense> findByTripGroupOrderByCreatedAtDesc(TripGroup tripGroup, Pageable pageable);

    @Query("SELECT DISTINCT e FROM Expense e LEFT JOIN FETCH e.participants p LEFT JOIN FETCH p.user WHERE e.tripGroup = :tripGroup")
    List<Expense> findByTripGroupWithParticipants(@Param("tripGroup") TripGroup tripGroup);
}

