package com.splitpush.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @Value("${keycloak.register-uri:https://auth.quangntran.com/realms/travel-platform/protocol/openid-connect/registrations}")
    private String keycloakRegisterUri;

    @Value("${app.base-url:https://splitpush.quangntran.com}")
    private String appBaseUrl;
    @GetMapping("/")
    public String index(Authentication authentication) {
        // If user is logged in, redirect to dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        // Otherwise, show home page
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    @GetMapping("/register")
    public String register() {
        String redirectUri = appBaseUrl + "/login/oauth2/code/keycloak";
        return "redirect:" + keycloakRegisterUri + "?client_id=splitpush&response_type=code&scope=openid&redirect_uri=" + redirectUri;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/groups")
    public String groups() {
        return "groups";
    }

    @GetMapping("/expenses")
    public String expenses() {
        return "expenses";
    }

    @GetMapping("/settlements")
    public String settlements() {
        return "settlements";
    }
}

