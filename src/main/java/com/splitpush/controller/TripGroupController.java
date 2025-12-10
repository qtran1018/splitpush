package com.splitpush.controller;

import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import com.splitpush.service.TripGroupService;
import com.splitpush.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class TripGroupController {
    @Autowired
    private TripGroupService tripGroupService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createTripGroup(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TripGroup group = tripGroupService.createTripGroup(
                    request.get("name"),
                    request.get("description"),
                    user.getId()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(group);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<List<TripGroup>> getMyTripGroups(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TripGroup> groups = tripGroupService.getTripGroupsByUser(user.getId());
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTripGroup(@PathVariable String id, Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return tripGroupService.getTripGroupById(id)
                .map(group -> {
                    // Check if user is a member of the group by comparing IDs
                    boolean isMember = group.getMembers().stream()
                            .anyMatch(member -> member.getId().equals(currentUser.getId()));
                    if (!isMember) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "You are not a member of this group");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
                    }
                    return ResponseEntity.ok(group);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable String groupId, @RequestBody Map<String, Long> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TripGroup group = tripGroupService.getTripGroupById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            // Check if current user is a member (only members can add others) - compare by ID
            boolean isMember = group.getMembers().stream()
                    .anyMatch(member -> member.getId().equals(currentUser.getId()));
            if (!isMember) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You must be a member of this group to add others");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            TripGroup updatedGroup = tripGroupService.addMemberToGroup(groupId, request.get("userId"));
            return ResponseEntity.ok(updatedGroup);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<?> joinGroup(@PathVariable String groupId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TripGroup group = tripGroupService.getTripGroupById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            // Check if user is already a member - compare by ID
            boolean isAlreadyMember = group.getMembers().stream()
                    .anyMatch(member -> member.getId().equals(currentUser.getId()));
            if (isAlreadyMember) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You are already a member of this group");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            TripGroup updatedGroup = tripGroupService.addMemberToGroup(groupId, currentUser.getId());
            return ResponseEntity.ok(updatedGroup);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable String groupId, @PathVariable Long userId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TripGroup group = tripGroupService.getTripGroupById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            // Only group creator or the user themselves can remove a member
            if (!group.getCreatedBy().getId().equals(currentUser.getId()) && !currentUser.getId().equals(userId)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You don't have permission to remove this member");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            TripGroup updatedGroup = tripGroupService.removeMemberFromGroup(groupId, userId);
            return ResponseEntity.ok(updatedGroup);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable String groupId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TripGroup updatedGroup = tripGroupService.leaveGroup(groupId, currentUser.getId());
            // If group deleted (no members left), return a simple message
            if (updatedGroup == null) {
                Map<String, String> resp = new HashMap<>();
                resp.put("message", "Group removed as no members remained.");
                return ResponseEntity.ok(resp);
            }
            return ResponseEntity.ok(updatedGroup);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(@PathVariable String groupId, @RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TripGroup updatedGroup = tripGroupService.updateGroup(groupId, request.get("name"), request.get("description"), currentUser.getId());
            return ResponseEntity.ok(updatedGroup);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

