package com.amiawake.amiawake.wakesubscription.controller;

import com.amiawake.amiawake.common.exception.GlobalExceptionHandler;
import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.common.exception.WakeSubscriptionAlreadyExistsException;
import com.amiawake.amiawake.common.exception.WakeSubscriptionForbiddenException;
import com.amiawake.amiawake.common.security.AuthenticatedUserIdResolver;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.wakesubscription.service.WakeSubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WakeSubscriptionControllerTest {
    private final UUID subscriberId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final UserService users = mock(UserService.class);
    private final WakeSubscriptionService subscriptions = mock(WakeSubscriptionService.class);
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(subscriberId.toString(), "unused");
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(users.getUserByUsername("friend"))
                .thenReturn(new User(targetId, "friend", "Friend", "hash", "UTC"));
        mvc = MockMvcBuilders.standaloneSetup(new WakeSubscriptionController(
                subscriptions, users, new AuthenticatedUserIdResolver()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void readsCreatesAndCancelsForAuthenticatedSubscriber() throws Exception {
        when(subscriptions.isSubscribed(subscriberId, targetId)).thenReturn(true);
        mvc.perform(get("/api/v1/wake-subscriptions/friend").principal(authentication))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subscribed").value(true));
        mvc.perform(post("/api/v1/wake-subscriptions/friend").principal(authentication))
                .andExpect(status().isNoContent());
        verify(subscriptions).subscribeToWake(subscriberId, targetId);
        mvc.perform(delete("/api/v1/wake-subscriptions/friend").principal(authentication))
                .andExpect(status().isNoContent());
        verify(subscriptions).removeWakeSubscription(subscriberId, targetId);
    }

    @Test
    void reportsForbiddenFriendshipAndDuplicateSubscription() throws Exception {
        when(subscriptions.isSubscribed(subscriberId, targetId)).thenThrow(new WakeSubscriptionForbiddenException());
        mvc.perform(get("/api/v1/wake-subscriptions/friend").principal(authentication))
                .andExpect(status().isForbidden());
        when(subscriptions.subscribeToWake(subscriberId, targetId)).thenThrow(new WakeSubscriptionAlreadyExistsException());
        mvc.perform(post("/api/v1/wake-subscriptions/friend").principal(authentication))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownUsernameDoesNotCreateSubscription() throws Exception {
        when(users.getUserByUsername("missing")).thenThrow(new UserNotFoundException("missing"));
        mvc.perform(post("/api/v1/wake-subscriptions/missing").principal(authentication))
                .andExpect(status().isNotFound());
        verifyNoInteractions(subscriptions);
    }
}
