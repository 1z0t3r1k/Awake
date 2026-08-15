package com.amiawake.amiawake.friendship.controller;

import com.amiawake.amiawake.friendship.dto.FriendRequest;
import com.amiawake.amiawake.friendship.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friendship")
public class FriendshipController {
    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendFriendRequest(@Valid @RequestBody FriendRequest request, Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());

        friendshipService.sendFriendRequest(requesterId, request.username());
    }

    @PostMapping("/{username}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptFriendRequest(@PathVariable String username, Authentication authentication) {
        UUID acceptorId = UUID.fromString(authentication.getName());

        friendshipService.acceptFriendRequest(acceptorId, username);
    }
}
