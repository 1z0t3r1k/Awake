package com.amiawake.amiawake.auth.controller;

import com.amiawake.amiawake.auth.dto.LoginRequest;
import com.amiawake.amiawake.auth.dto.LoginResponse;
import com.amiawake.amiawake.auth.dto.LogoutRequest;
import com.amiawake.amiawake.auth.dto.RefreshRequest;
import com.amiawake.amiawake.auth.dto.RefreshResponse;
import com.amiawake.amiawake.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public RefreshResponse refreshToken(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }
}
