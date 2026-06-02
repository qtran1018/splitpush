package com.splitpush.controller;

import com.splitpush.model.InviteToken;
import com.splitpush.model.TripGroup;
import com.splitpush.model.User;
import com.splitpush.service.InviteService;
import com.splitpush.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class InviteController {

    @Autowired
    private InviteService inviteService;

    @Autowired
    private UserService userService;

    /** Public landing page shown before login. */
    @GetMapping("/invite/{token}")
    public String invitePage(@PathVariable String token, Model model, Authentication authentication) {
        try {
            InviteToken invite = inviteService.getInvite(token);
            model.addAttribute("token", token);
            model.addAttribute("groupName", invite.getTripGroup().getName());
            model.addAttribute("inviterName", invite.getCreatedBy().getName());
            model.addAttribute("authenticated", authentication != null && authentication.isAuthenticated());
            return "invite";
        } catch (RuntimeException e) {
            model.addAttribute("error", "This invite link is invalid or has expired.");
            return "invite";
        }
    }

    /** Called after the user is authenticated — performs the actual join.
     *  GET is intentional: Spring Security's saved-request mechanism replays GET requests
     *  after login, enabling unauthenticated users to complete the join flow via /invite/{token}/join.
     */
    @GetMapping("/invite/{token}/join")
    public String joinViaInvite(@PathVariable String token, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            TripGroup group = inviteService.joinViaInvite(token, user.getId());
            return "redirect:/expenses?group=" + group.getId();
        } catch (RuntimeException e) {
            return "redirect:/groups?error=" + e.getMessage();
        }
    }

    /** REST endpoint for creating an invite (called from groups.js). */
    @ResponseBody
    @PostMapping("/api/invite")
    public ResponseEntity<?> createInvite(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            InviteToken invite = inviteService.createInvite(request.get("groupId"), user.getId());
            Map<String, String> resp = new HashMap<>();
            resp.put("token", invite.getToken());
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
