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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.username()
                .toLowerCase(Locale.ROOT);
        String password = request.password();

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = refreshTokenService.createRefreshToken(user);
        String tokenType = "Bearer";
        long expiresIn = jwtProperties.accessTokenTtl()
                .getSeconds();

        return new LoginResponse(accessToken, refreshToken, tokenType, expiresIn);
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken =
                refreshTokenService.validateRefreshToken(request.refreshToken());
        User user = refreshToken.getUser();
        String newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);

        String accessToken = jwtService.generateAccessToken(user.getId());
        String tokenType = "Bearer";
        long expiresIn = jwtProperties.accessTokenTtl().getSeconds();

        return new RefreshResponse(accessToken, newRefreshToken, tokenType, expiresIn);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken());

        refreshToken.revoke();
    }
}
