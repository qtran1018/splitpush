package com.splitpush.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Registration is now handled by Keycloak.
// Users are provisioned locally on first OIDC login via KeycloakOidcUserService.
@RestController
@RequestMapping("/api/auth")
public class AuthController {
}
