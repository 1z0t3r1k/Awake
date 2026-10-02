package com.amiawake.amiawake.wakesubscription.service;

import com.amiawake.amiawake.common.exception.WakeSubscriptionForbiddenException;
import com.amiawake.amiawake.friendship.service.FriendshipService;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.wakesubscription.repository.WakeSubscriptionRepository;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WakeSubscriptionServiceTest {
    @Test
    void cannotReadSubscriptionForSomeoneWhoIsNotAnAcceptedFriend() {
        var subscriber = new User(UUID.randomUUID(), "alice", "Alice", "hash", "UTC");
        var target = new User(UUID.randomUUID(), "friend", "Friend", "hash", "UTC");
        var users = mock(UserService.class);
        var friendships = mock(FriendshipService.class);
        var repository = mock(WakeSubscriptionRepository.class);
        when(users.getUserById(subscriber.getId())).thenReturn(subscriber);
        when(users.getUserById(target.getId())).thenReturn(target);
        var service = new WakeSubscriptionService(repository, friendships, users);
        assertThrows(WakeSubscriptionForbiddenException.class,
                () -> service.isSubscribed(subscriber.getId(), target.getId()));
        verifyNoInteractions(repository);
    }
}
