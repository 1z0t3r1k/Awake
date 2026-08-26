package com.amiawake.amiawake.user.controller;

import com.amiawake.amiawake.user.dto.StatusRequest;
import com.amiawake.amiawake.user.dto.StatusResponse;
import com.amiawake.amiawake.user.dto.TimeZoneRequest;
import com.amiawake.amiawake.user.dto.UserCreateRequest;
import com.amiawake.amiawake.user.dto.UserResponse;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.mapper.UserMapper;
import com.amiawake.amiawake.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getUser(Authentication authentication) {
        UUID id = UUID.fromString(authentication.getName());
        User user = userService.getUserById(id);

        return UserMapper.toResponse(user);
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserMapper.toResponse(user));
    }

    @PatchMapping("/me/status")
    public StatusResponse changeStatus(@RequestBody @Valid StatusRequest request, Authentication authentication) {
        UUID id = UUID.fromString(authentication.getName());

        return userService.changeStatus(id, request.status());
    }

    @GetMapping("/me/status")
    public StatusResponse getStatus(Authentication authentication) {
        UUID id = UUID.fromString(authentication.getName());

        return userService.getStatus(id);
    }

    @PatchMapping("me/time-zone")
    public void changeTimeZone(Authentication authentication, @Valid @RequestBody TimeZoneRequest request) {
        UUID userId = UUID.fromString(authentication.getName());

        userService.changeTimeZone(userId, request);
    }
}
