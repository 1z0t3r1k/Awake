package com.amiawake.amiawake.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticatedUserIdResolver {

    public UUID resolve(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
