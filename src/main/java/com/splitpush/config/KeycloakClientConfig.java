package com.splitpush.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Map;

/**
 * Manually registers the Keycloak OAuth2 client so all endpoint URLs can be configured
 * independently without requiring OIDC autodiscovery at startup.
 *
 * Why: Spring Boot's OAuth2 auto-configuration fetches the OIDC discovery document at
 * startup when issuer-uri is set.  Inside Docker the app can only reach Keycloak via its
 * container name (keycloak:8080), but the browser must use localhost:8180.  These two URLs
 * produce tokens with different iss claims, making issuer validation impossible without
 * KC_HOSTNAME plus host-reachable discovery.  Providing this bean makes Spring Boot skip
 * the auto-configuration entirely (ConditionalOnMissingBean), and because no issuer is set
 * on the ClientRegistration, OidcIdTokenValidator skips the iss claim check at runtime while
 * still validating JWT signatures via the jwk-set-uri.
 */
@Configuration
public class KeycloakClientConfig {

    @Value("${keycloak.auth-uri:http://localhost:8180/realms/travel-platform/protocol/openid-connect/auth}")
    private String authUri;

    @Value("${keycloak.token-uri:http://localhost:8180/realms/travel-platform/protocol/openid-connect/token}")
    private String tokenUri;

    // JWKS endpoint is a public key fetch — no issuer validation, so internal network is fine.
    @Value("${keycloak.jwk-uri:http://localhost:8180/realms/travel-platform/protocol/openid-connect/certs}")
    private String jwkUri;

    // RP-Initiated Logout — the browser redirects here to terminate the Keycloak SSO session.
    // Must be browser-accessible (localhost:8180), not the Docker-internal URL.
    @Value("${keycloak.end-session-uri:http://localhost:8180/realms/travel-platform/protocol/openid-connect/logout}")
    private String endSessionUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret:splitpush-secret}")
    private String clientSecret;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("keycloak")
                .clientId("splitpush")
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(authUri)
                .tokenUri(tokenUri)
                // No userInfoUri — OidcUserService.shouldRetrieveUserInfo() returns false,
                // so all user claims come from the ID token (which carries email, preferred_username,
                // name etc. when profile+email scope is requested). Avoids the issuer mismatch
                // that occurs when calling UserInfo from inside Docker.
                .jwkSetUri(jwkUri)
                .userNameAttributeName("email")
                .clientName("Keycloak")
                // OidcClientInitiatedLogoutSuccessHandler reads end_session_endpoint from here.
                .providerConfigurationMetadata(Map.of("end_session_endpoint", endSessionUri))
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
