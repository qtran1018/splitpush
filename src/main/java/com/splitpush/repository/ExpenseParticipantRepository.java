package com.splitpush.repository;

import com.splitpush.model.ExpenseParticipant;
import com.splitpush.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {
    List<ExpenseParticipant> findByUser(User user);
    List<ExpenseParticipant> findByUserAndIsPaidFalse(User user);
}

