package com.splitpush.service;

import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import com.splitpush.repository.TripGroupRepository;
import com.splitpush.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitpush.dto.BalanceDTO;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@SuppressWarnings("null")
public class TripGroupService {
    @Autowired
    private TripGroupRepository tripGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.splitpush.service.ExpenseService expenseService;

    @CacheEvict(value = {"tripGroups", "tripGroupById", "userGroups", "expensesByGroup", "balances"}, allEntries = true)
    public TripGroup createTripGroup(String name, String description, Long createdById) {
        User createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TripGroup tripGroup = new TripGroup();
        // ID will be generated in @PrePersist
        tripGroup.setName(name);
        tripGroup.setDescription(description);
        tripGroup.setCreatedBy(createdBy);
        tripGroup.getMembers().add(createdBy); // Creator is automatically a member

        return tripGroupRepository.save(tripGroup);
    }

    @Cacheable(value = "userGroups", key = "#userId")
    public List<TripGroup> getTripGroupsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return tripGroupRepository.findByMembersContaining(user);
    }

    // Not cached - needs fresh data for authentication checks and lazy-loaded relationships
    public Optional<TripGroup> getTripGroupById(String id) {
        return tripGroupRepository.findById(id);
    }

    @CacheEvict(value = {"tripGroups", "tripGroupById", "userGroups", "expensesByGroup", "balances"}, allEntries = true)
    public TripGroup addMemberToGroup(String groupId, Long userId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Trip group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check membership by ID to avoid object equality issues
        boolean isAlreadyMember = group.getMembers().stream()
                .anyMatch(member -> member.getId().equals(user.getId()));
        if (isAlreadyMember) {
            throw new RuntimeException("User is already a member of this group");
        }

        group.getMembers().add(user);
        return tripGroupRepository.save(group);
    }

    @CacheEvict(value = {"tripGroups", "tripGroupById", "userGroups", "expensesByGroup", "balances"}, allEntries = true)
    public TripGroup removeMemberFromGroup(String groupId, Long userId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Trip group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Don't allow removing the creator
        if (group.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("Cannot remove the group creator");
        }

        group.getMembers().remove(user);
        return tripGroupRepository.save(group);
    }

    @CacheEvict(value = {"tripGroups", "tripGroupById", "userGroups", "expensesByGroup", "balances", "settlements"}, allEntries = true)
    public TripGroup leaveGroup(String groupId, Long userId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Trip group not found"));
        boolean isMember = group.getMembers().stream().anyMatch(m -> m.getId().equals(userId));
        if (!isMember) {
            throw new RuntimeException("You are not a member of this group");
        }

        // Prevent leaving if any balances remain in this group (owed or owing).
        // calculateBalances is cached, so this is at most one DB round-trip.
        List<BalanceDTO> balances = expenseService.calculateBalances(userId);
        boolean hasUnpaidBalance = balances.stream()
                .flatMap(b -> b.getGroupBreakdownDetails().stream())
                .anyMatch(b -> b.getGroupId().equals(groupId)
                        && b.getAmount().compareTo(java.math.BigDecimal.ZERO) != 0);
        if (hasUnpaidBalance) {
            throw new RuntimeException("You must settle all balances in this group before leaving.");
        }

        group.getMembers().removeIf(m -> m.getId().equals(userId));

        if (group.getMembers().isEmpty()) {
            // If no members remain, remove the group entirely
            tripGroupRepository.delete(group);
            return null;
        }

        return tripGroupRepository.save(group);
    }

    @CacheEvict(value = {"tripGroups", "tripGroupById", "userGroups", "expensesByGroup", "balances"}, allEntries = true)
    public TripGroup updateGroup(String groupId, String name, String description, Long userId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Trip group not found"));
        
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if user is a member of the group (any member can edit name and description) - compare by ID
        boolean isMember = group.getMembers().stream()
                .anyMatch(member -> member.getId().equals(userId));
        if (!isMember) {
            throw new RuntimeException("You must be a member of this group to update it");
        }
        
        if (name != null && !name.trim().isEmpty()) {
            group.setName(name.trim());
        }
        
        if (description != null) {
            group.setDescription(description);
        }
        
        return tripGroupRepository.save(group);
    }
}

