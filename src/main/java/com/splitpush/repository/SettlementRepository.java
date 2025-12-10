package com.splitpush.repository;

import com.splitpush.model.Settlement;
import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByTripGroup(TripGroup tripGroup);
    List<Settlement> findByPayer(User payer);
    List<Settlement> findByPayee(User payee);
}

