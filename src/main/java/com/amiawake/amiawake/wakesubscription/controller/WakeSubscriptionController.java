package com.amiawake.amiawake.wakesubscription.controller;

import com.amiawake.amiawake.common.security.AuthenticatedUserIdResolver;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.wakesubscription.dto.WakeSubscriptionResponse;
import com.amiawake.amiawake.wakesubscription.service.WakeSubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wake-subscriptions")
public class WakeSubscriptionController {
    private final WakeSubscriptionService subscriptions;
    private final UserService users;
    private final AuthenticatedUserIdResolver userIds;

    public WakeSubscriptionController(WakeSubscriptionService subscriptions, UserService users,
                                      AuthenticatedUserIdResolver userIds) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.userIds = userIds;
    }

    @GetMapping("/{username}")
    public WakeSubscriptionResponse get(@PathVariable String username, Authentication authentication) {
        return new WakeSubscriptionResponse(subscriptions.isSubscribed(userIds.resolve(authentication), targetId(username)));
    }

    @PostMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@PathVariable String username, Authentication authentication) {
        subscriptions.subscribeToWake(userIds.resolve(authentication), targetId(username));
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable String username, Authentication authentication) {
        subscriptions.removeWakeSubscription(userIds.resolve(authentication), targetId(username));
    }

    private UUID targetId(String username) {
        return users.getUserByUsername(username).getId();
    }
}
