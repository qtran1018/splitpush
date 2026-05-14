package com.splitpush.service;

import com.splitpush.model.User;
import com.splitpush.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class KeycloakOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String preferredUsername = oidcUser.getPreferredUsername();
        String fullName = oidcUser.getFullName();

        userRepository.findByEmail(email).orElseGet(() -> {
            String base = (preferredUsername != null ? preferredUsername : email.split("@")[0])
                    .replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
            String username = base;
            int i = 1;
            while (userRepository.existsByUsername(username)) {
                username = base + i++;
            }
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setName(fullName != null && !fullName.isBlank() ? fullName : username);
            return userRepository.save(user);
        });

        return oidcUser;
    }
}
