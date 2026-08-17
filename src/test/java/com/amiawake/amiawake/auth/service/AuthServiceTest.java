package com.amiawake.amiawake.auth.service;

import com.amiawake.amiawake.auth.dto.LoginRequest;
import com.amiawake.amiawake.auth.dto.LoginResponse;
import com.amiawake.amiawake.auth.dto.LogoutRequest;
import com.amiawake.amiawake.auth.dto.RefreshRequest;
import com.amiawake.amiawake.auth.dto.RefreshResponse;
import com.amiawake.amiawake.auth.entity.RefreshToken;
import com.amiawake.amiawake.common.exception.InvalidCredentialsException;
import com.amiawake.amiawake.common.security.JwtProperties;
import com.amiawake.amiawake.common.security.JwtService;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(null, null, Duration.ofMinutes(15));
        authService = new AuthService(userRepository, refreshTokenService, passwordEncoder, jwtService, jwtProperties);
    }

    @Test
    void loginLowercasesUsernameAndReturnsTokens() {
        User user = user("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "password-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user.getId())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        LoginResponse response = authService.login(new LoginRequest("Alice", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        verify(userRepository).findByUsername("alice");
    }

    @Test
    void loginRejectsUnknownUsername() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = user("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void refreshValidatesRotatesAndReturnsNewTokens() {
        User user = user("alice");
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user, "hash", Instant.now().plusSeconds(60));
        when(refreshTokenService.validateRefreshToken("old-refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.rotateRefreshToken(refreshToken)).thenReturn("new-refresh-token");
        when(jwtService.generateAccessToken(user.getId())).thenReturn("new-access-token");

        RefreshResponse response = authService.refresh(new RefreshRequest("old-refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void logoutRevokesValidatedRefreshToken() {
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user("alice"), "hash", Instant.now().plusSeconds(60));
        when(refreshTokenService.validateRefreshToken("refresh-token")).thenReturn(refreshToken);

        authService.logout(new LogoutRequest("refresh-token"));

        assertThat(refreshToken.getRevokedAt()).isNotNull();
    }

    private static User user(String username) {
        return new User(UUID.randomUUID(), username, username, "password-hash", "UTC");
    }
}
