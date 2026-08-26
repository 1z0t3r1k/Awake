package com.amiawake.amiawake.friendship.controller;

import com.amiawake.amiawake.friendship.dto.FriendRequest;
import com.amiawake.amiawake.friendship.dto.FriendResponse;
import com.amiawake.amiawake.friendship.dto.IncomingFriendRequestResponse;
import com.amiawake.amiawake.friendship.dto.OutgoingFriendRequestResponse;
import com.amiawake.amiawake.friendship.service.FriendshipService;
import com.amiawake.amiawake.userstate.dto.UserStateResponse;
import com.amiawake.amiawake.userstate.service.UserStateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friendship")
public class FriendshipController {
    private final FriendshipService friendshipService;
    private final UserStateService userStateService;

    public FriendshipController(FriendshipService friendshipService, UserStateService userStateService) {
        this.friendshipService = friendshipService;
        this.userStateService = userStateService;
    }

    private UUID getIdByAuthentication(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendFriendRequest(@Valid @RequestBody FriendRequest request, Authentication authentication) {
        UUID requesterId = getIdByAuthentication(authentication);

        friendshipService.sendFriendRequest(requesterId, request.username());
    }

    @PostMapping("/{username}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptFriendRequest(@PathVariable String username, Authentication authentication) {
        UUID acceptorId = getIdByAuthentication(authentication);

        friendshipService.acceptFriendRequest(acceptorId, username);
    }

    @GetMapping("/requests/incoming")
    public List<IncomingFriendRequestResponse> getIncomingFriendRequests(Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        return friendshipService.getIncomingFriendRequests(userId);
    }

    @GetMapping
    public List<FriendResponse> getFriends(Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        return friendshipService.getFriends(userId);
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriend(@PathVariable String username, Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        friendshipService.deleteFriend(userId, username);
    }

    @DeleteMapping("/requests/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePendingRequest(@PathVariable String username, Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        friendshipService.deletePendingRequest(userId, username);
    }

    @GetMapping("/requests/outgoing")
    public List<OutgoingFriendRequestResponse> getOutgoingFriendRequests(Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        return friendshipService.getOutgoingFriendRequests(userId);
    }

    @GetMapping("/{username}/state")
    public UserStateResponse getFriendState(Authentication authentication, @PathVariable String username) {
        UUID userId = getIdByAuthentication(authentication);

        return userStateService.getFriendState(userId, username);
    }
}
