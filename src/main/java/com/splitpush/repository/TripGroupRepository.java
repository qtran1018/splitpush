package com.splitpush.repository;

import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripGroupRepository extends JpaRepository<TripGroup, String> {
    List<TripGroup> findByMembersContaining(User user);
    List<TripGroup> findByCreatedBy(User user);
}

