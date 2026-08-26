package com.amiawake.amiawake.userstate.controller;

import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.userstate.dto.UserStateResponse;
import com.amiawake.amiawake.userstate.service.UserStateService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-state")
public class UserStateController {

    private final UserStateService userStateService;
    private final UserService userService;

    public UserStateController(
            UserStateService userStateService,
            UserService userService
    ) {
        this.userStateService = userStateService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserStateResponse getMyState(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        User user = userService.getUserById(userId);

        return userStateService.getUserState(user);
    }
}