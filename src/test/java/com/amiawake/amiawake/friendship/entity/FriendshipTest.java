package com.amiawake.amiawake.friendship.entity;

import com.amiawake.amiawake.common.exception.CannotAcceptOwnFriendRequestException;
import com.amiawake.amiawake.common.exception.CannotFriendYourselfException;
import com.amiawake.amiawake.common.exception.FriendshipNotPendingException;
import com.amiawake.amiawake.common.exception.UserNotPartOfFriendshipException;
import com.amiawake.amiawake.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriendshipTest {

    private final User lowerIdUser = user("00000000-0000-0000-0000-000000000001", "alice");
    private final User higherIdUser = user("00000000-0000-0000-0000-000000000002", "bob");
    private final User outsider = user("00000000-0000-0000-0000-000000000003", "carol");

    @Test
    void constructorNormalizesUserOrderAndCreatesPendingRequest() {
        Friendship friendship = new Friendship(higherIdUser, lowerIdUser, higherIdUser);

        assertThat(friendship.getId()).isNotNull();
        assertThat(friendship.getUser1()).isSameAs(lowerIdUser);
        assertThat(friendship.getUser2()).isSameAs(higherIdUser);
        assertThat(friendship.getRequester()).isSameAs(higherIdUser);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(friendship.getCreatedAt()).isNotNull();
        assertThat(friendship.getUpdatedAt()).isNotNull();
    }

    @Test
    void constructorRejectsRequesterOutsidePair() {
        assertThatThrownBy(() -> new Friendship(lowerIdUser, higherIdUser, outsider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Requester must be one of the users");
    }

    @Test
    void constructorRejectsSameUserPair() {
        assertThatThrownBy(() -> new Friendship(lowerIdUser, lowerIdUser, lowerIdUser))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizeUsersReturnsLowerUuidFirst() {
        FriendshipPair pair = Friendship.normalizeUsers(higherIdUser, lowerIdUser);

        assertThat(pair.firstUser()).isSameAs(lowerIdUser);
        assertThat(pair.secondUser()).isSameAs(higherIdUser);
    }

    @Test
    void normalizeUsersRejectsSameUser() {
        assertThatThrownBy(() -> Friendship.normalizeUsers(lowerIdUser, lowerIdUser))
                .isInstanceOf(CannotFriendYourselfException.class);
    }

    @Test
    void receiverCanAcceptPendingFriendship() {
        Friendship friendship = new Friendship(lowerIdUser, higherIdUser, lowerIdUser);
        Instant previousUpdatedAt = friendship.getUpdatedAt();

        friendship.acceptFriendship(higherIdUser);

        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
        assertThat(friendship.getUpdatedAt()).isAfterOrEqualTo(previousUpdatedAt);
    }

    @Test
    void requesterCannotAcceptOwnRequest() {
        Friendship friendship = new Friendship(lowerIdUser, higherIdUser, lowerIdUser);

        assertThatThrownBy(() -> friendship.acceptFriendship(lowerIdUser))
                .isInstanceOf(CannotAcceptOwnFriendRequestException.class);
    }

    @Test
    void outsiderCannotAcceptFriendship() {
        Friendship friendship = new Friendship(lowerIdUser, higherIdUser, lowerIdUser);

        assertThatThrownBy(() -> friendship.acceptFriendship(outsider))
                .isInstanceOf(UserNotPartOfFriendshipException.class);
    }

    @Test
    void acceptedFriendshipCannotBeAcceptedAgain() {
        Friendship friendship = new Friendship(lowerIdUser, higherIdUser, lowerIdUser);
        friendship.acceptFriendship(higherIdUser);

        assertThatThrownBy(() -> friendship.acceptFriendship(higherIdUser))
                .isInstanceOf(FriendshipNotPendingException.class);
    }

    @Test
    void getOtherUserReturnsOppositeParticipant() {
        Friendship friendship = new Friendship(lowerIdUser, higherIdUser, lowerIdUser);

        assertThat(friendship.getOtherUser(lowerIdUser)).isSameAs(higherIdUser);
        assertThat(friendship.getOtherUser(higherIdUser)).isSameAs(lowerIdUser);
    }

    @Test
    void getOtherUserRejectsOutsider() {
        Friendship friendship = new Friendship(lowerIdUser, higherIdUser, lowerIdUser);

        assertThatThrownBy(() -> friendship.getOtherUser(outsider))
                .isInstanceOf(UserNotPartOfFriendshipException.class);
    }

    private static User user(String id, String username) {
        return new User(UUID.fromString(id), username, username, "hash", "UTC");
    }
}
