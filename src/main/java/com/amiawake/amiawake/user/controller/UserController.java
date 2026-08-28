package com.amiawake.amiawake.user.controller;

import com.amiawake.amiawake.user.dto.DisplayNameRequest;
import com.amiawake.amiawake.user.dto.StatusRequest;
import com.amiawake.amiawake.user.dto.StatusResponse;
import com.amiawake.amiawake.user.dto.TimeZoneRequest;
import com.amiawake.amiawake.user.dto.UserCreateRequest;
import com.amiawake.amiawake.user.dto.UserResponse;
import com.amiawake.amiawake.user.dto.UserSearchResponse;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.mapper.UserMapper;
import com.amiawake.amiawake.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private UUID getUserByAuthentication(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping("/me")
    public UserResponse getUser(Authentication authentication) {
        UUID id = getUserByAuthentication(authentication);
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
        UUID id = getUserByAuthentication(authentication);

        return userService.changeStatus(id, request.status());
    }

    @GetMapping("/me/status")
    public StatusResponse getStatus(Authentication authentication) {
        UUID id = getUserByAuthentication(authentication);

        return userService.getStatus(id);
    }

    @PatchMapping("/me/time-zone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeTimeZone(Authentication authentication, @RequestBody @Valid TimeZoneRequest request) {
        UUID userId = getUserByAuthentication(authentication);

        userService.changeTimeZone(userId, request);
    }

    @PatchMapping("/me/display-name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDisplayName(Authentication authentication, @RequestBody @Valid DisplayNameRequest request) {
        UUID userId = getUserByAuthentication(authentication);

        userService.changeDisplayName(userId, request);
    }

    @GetMapping("/search")
    public List<UserSearchResponse> searchUsers(Authentication authentication, @RequestParam @NotBlank String query) {
        UUID userId = getUserByAuthentication(authentication);

        return userService.searchUsers(userId, query.strip());
    }
}
