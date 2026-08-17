package com.amiawake.amiawake.auth.service;

import com.amiawake.amiawake.auth.entity.RefreshToken;
import com.amiawake.amiawake.auth.properties.RefreshTokenProperties;
import com.amiawake.amiawake.auth.repository.RefreshTokenRepository;
import com.amiawake.amiawake.common.exception.InvalidRefreshTokenException;
import com.amiawake.amiawake.common.security.JwtProperties;
import com.amiawake.amiawake.common.security.JwtService;
import com.amiawake.amiawake.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties(Duration.ofDays(30));
        JwtProperties jwtProperties = new JwtProperties(null, null, Duration.ofMinutes(15));
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, refreshTokenProperties, jwtService, jwtProperties);
    }

    @Test
    void createRefreshTokenSavesHashAndReturnsRawToken() {
        User user = user("alice");

        String rawToken = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(rawToken).isNotBlank();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isNotBlank();
        assertThat(savedToken.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(29)));
        assertThat(savedToken.getCreatedAt()).isNotNull();
    }

    @Test
    void validateRefreshTokenReturnsActiveUnexpiredToken() {
        User user = user("alice");
        String rawToken = refreshTokenService.createRefreshToken(user);
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        when(refreshTokenRepository.findByTokenHash(savedToken.getTokenHash())).thenReturn(Optional.of(savedToken));

        RefreshToken result = refreshTokenService.validateRefreshToken(rawToken);

        assertThat(result).isSameAs(savedToken);
    }

    @Test
    void validateRefreshTokenRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("missing-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateRefreshTokenRejectsExpiredToken() {
        RefreshToken expiredToken = new RefreshToken(UUID.randomUUID(), user("alice"), "hash", Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateRefreshTokenRejectsRevokedToken() {
        RefreshToken revokedToken = new RefreshToken(UUID.randomUUID(), user("alice"), "hash", Instant.now().plusSeconds(60));
        revokedToken.revoke();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateRefreshTokenReplacesStoredHashAndExtendsExpiration() {
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user("alice"), "old-hash", Instant.now().plusSeconds(60));

        String newRawToken = refreshTokenService.rotateRefreshToken(refreshToken);

        assertThat(newRawToken).isNotBlank();
        assertThat(refreshToken.getTokenHash()).isNotEqualTo("old-hash");
        assertThat(refreshToken.getTokenHash()).isNotEqualTo(newRawToken);
        assertThat(refreshToken.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(29)));
    }

    private static User user(String username) {
        return new User(UUID.randomUUID(), username, username, "hash", "UTC");
    }
}
