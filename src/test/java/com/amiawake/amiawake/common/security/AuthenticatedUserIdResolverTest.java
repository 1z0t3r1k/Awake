package com.amiawake.amiawake.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserIdResolverTest {

    private final AuthenticatedUserIdResolver resolver = new AuthenticatedUserIdResolver();

    @Test
    void resolvesUserIdFromAuthenticationName() {
        UUID userId = UUID.randomUUID();
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());

        assertThat(resolver.resolve(authentication)).isEqualTo(userId);
    }
}
