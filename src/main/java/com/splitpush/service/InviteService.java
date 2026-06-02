package com.splitpush.service;

import com.splitpush.model.InviteToken;
import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import com.splitpush.repository.InviteTokenRepository;
import com.splitpush.repository.TripGroupRepository;
import com.splitpush.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InviteService {

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private TripGroupRepository tripGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripGroupService tripGroupService;

    public InviteToken createInvite(String groupId, Long userId) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isMember = group.getMembers().stream()
                .anyMatch(m -> m.getId().equals(userId));
        if (!isMember) {
            throw new RuntimeException("You must be a member to create an invite");
        }

        InviteToken invite = new InviteToken();
        invite.setTripGroup(group);
        invite.setCreatedBy(user);
        return inviteTokenRepository.save(invite);
    }

    public InviteToken getInvite(String token) {
        return inviteTokenRepository.findByTokenWithDetails(token)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
    }

    public TripGroup joinViaInvite(String token, Long userId) {
        InviteToken invite = inviteTokenRepository.findByTokenWithDetails(token)
                .orElseThrow(() -> new RuntimeException("Invite not found or expired"));

        String groupId = invite.getTripGroup().getId();
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group no longer exists"));

        boolean isAlreadyMember = group.getMembers().stream()
                .anyMatch(m -> m.getId().equals(userId));
        if (isAlreadyMember) {
            return group;
        }

        return tripGroupService.addMemberToGroup(groupId, userId);
    }
}
