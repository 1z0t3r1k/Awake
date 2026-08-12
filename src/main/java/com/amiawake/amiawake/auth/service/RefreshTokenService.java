package com.amiawake.amiawake.auth.service;

import com.amiawake.amiawake.auth.entity.RefreshToken;
import com.amiawake.amiawake.auth.properties.RefreshTokenProperties;
import com.amiawake.amiawake.auth.repository.RefreshTokenRepository;
import com.amiawake.amiawake.common.exception.InvalidRefreshTokenException;
import com.amiawake.amiawake.common.security.JwtProperties;
import com.amiawake.amiawake.common.security.JwtService;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository, RefreshTokenProperties refreshTokenProperties, JwtService jwtService,
            JwtProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenProperties = refreshTokenProperties;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = messageDigest.digest(tokenBytes);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public String createRefreshToken(User user) {
        String token = generateToken();
        String tokenHash = hashToken(token);
        Instant expiresAt = Instant.now()
                .plus(refreshTokenProperties.ttl());
        RefreshToken refreshTokenEntity = new RefreshToken(
                UUID.randomUUID(),
                user,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(refreshTokenEntity);

        return token;
    }

    public RefreshToken validateRefreshToken(String token) {
        String tokenHash = hashToken(token);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.getRevokedAt() != null
                || !Instant.now().isBefore(refreshToken.getExpiresAt())) {
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    public String rotateRefreshToken(RefreshToken refreshToken) {
        String newRefreshToken = generateToken();
        String newRefreshTokenHash = hashToken(newRefreshToken);
        Instant newExpiresAt = Instant.now().plus(refreshTokenProperties.ttl());

        refreshToken.rotate(newRefreshTokenHash, newExpiresAt);

        return newRefreshToken;
    }
}
