package com.amiawake.amiawake.friendship.service;

import com.amiawake.amiawake.common.exception.CannotAcceptOwnFriendRequestException;
import com.amiawake.amiawake.common.exception.CannotFriendYourselfException;
import com.amiawake.amiawake.common.exception.FriendshipAlreadyExistsException;
import com.amiawake.amiawake.common.exception.FriendshipDoesNotExistException;
import com.amiawake.amiawake.common.exception.FriendshipNotAcceptedException;
import com.amiawake.amiawake.common.exception.FriendshipNotPendingException;
import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.friendship.dto.FriendResponse;
import com.amiawake.amiawake.friendship.dto.IncomingFriendRequestResponse;
import com.amiawake.amiawake.friendship.dto.OutgoingFriendRequestResponse;
import com.amiawake.amiawake.friendship.entity.Friendship;
import com.amiawake.amiawake.friendship.entity.FriendshipPair;
import com.amiawake.amiawake.friendship.entity.FriendshipStatus;
import com.amiawake.amiawake.friendship.repository.FriendshipRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    private final User alice = user("00000000-0000-0000-0000-000000000001", "alice");
    private final User bob = user("00000000-0000-0000-0000-000000000002", "bob");
    private final User carol = user("00000000-0000-0000-0000-000000000003", "carol");

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    private FriendshipService friendshipService;

    @BeforeEach
    void setUp() {
        friendshipService = new FriendshipService(friendshipRepository, userRepository);
    }

    @Test
    void sendFriendRequestCreatesPendingFriendship() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.empty());

        friendshipService.sendFriendRequest(alice.getId(), "bob");

        ArgumentCaptor<Friendship> friendshipCaptor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(friendshipCaptor.capture());
        Friendship savedFriendship = friendshipCaptor.getValue();
        assertThat(savedFriendship.getUser1()).isSameAs(alice);
        assertThat(savedFriendship.getUser2()).isSameAs(bob);
        assertThat(savedFriendship.getRequester()).isSameAs(alice);
        assertThat(savedFriendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    }

    @Test
    void sendFriendRequestRejectsMissingReceiver() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(alice.getId(), "missing"))
                .isInstanceOf(UserNotFoundException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequestRejectsSelfFriendship() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(alice.getId(), "alice"))
                .isInstanceOf(CannotFriendYourselfException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void sendFriendRequestRejectsExistingFriendship() {
        Friendship existingFriendship = new Friendship(alice, bob, alice);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.of(existingFriendship));

        assertThatThrownBy(() -> friendshipService.sendFriendRequest(alice.getId(), "bob"))
                .isInstanceOf(FriendshipAlreadyExistsException.class);

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void acceptFriendRequestAcceptsPendingRequest() {
        Friendship friendship = new Friendship(alice, bob, alice);
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.of(friendship));

        friendshipService.acceptFriendRequest(bob.getId(), "alice");

        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void acceptFriendRequestRejectsOwnRequest() {
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(alice.getId(), "alice"))
                .isInstanceOf(CannotAcceptOwnFriendRequestException.class);
    }

    @Test
    void acceptFriendRequestRejectsMissingFriendship() {
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(bob.getId(), "alice"))
                .isInstanceOf(FriendshipDoesNotExistException.class);
    }

    @Test
    void getIncomingFriendRequestsReturnsRequesterUsernames() {
        Friendship aliceToBob = new Friendship(alice, bob, alice);
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(friendshipRepository.findIncomingRequests(bob, FriendshipStatus.PENDING)).thenReturn(List.of(aliceToBob));

        List<IncomingFriendRequestResponse> response = friendshipService.getIncomingFriendRequests(bob.getId());

        assertThat(response).extracting(IncomingFriendRequestResponse::username).containsExactly("alice");
    }

    @Test
    void getOutgoingFriendRequestsReturnsReceivers() {
        Friendship aliceToBob = new Friendship(alice, bob, alice);
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(friendshipRepository.findOutgoingRequests(alice, FriendshipStatus.PENDING)).thenReturn(List.of(aliceToBob));

        List<OutgoingFriendRequestResponse> response = friendshipService.getOutgoingFriendRequests(alice.getId());

        assertThat(response).extracting(OutgoingFriendRequestResponse::username).containsExactly("bob");
    }

    @Test
    void getFriendsReturnsOtherAcceptedParticipantsWithStatus() {
        bob.changeStatus(User.AvailabilityStatus.TEXT_ONLY);
        Friendship friendship = new Friendship(alice, bob, alice);
        friendship.acceptFriendship(bob);
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(friendshipRepository.findFriendships(alice, FriendshipStatus.ACCEPTED)).thenReturn(List.of(friendship));

        List<FriendResponse> response = friendshipService.getFriends(alice.getId());

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().username()).isEqualTo("bob");
        assertThat(response.getFirst().status()).isEqualTo(User.AvailabilityStatus.TEXT_ONLY);
    }

    @Test
    void deleteFriendDeletesAcceptedFriendship() {
        Friendship friendship = acceptedFriendship(alice, bob, alice, bob);
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.of(friendship));

        friendshipService.deleteFriend(alice.getId(), "bob");

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    void deleteFriendRejectsPendingFriendship() {
        Friendship friendship = new Friendship(alice, bob, alice);
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.deleteFriend(alice.getId(), "bob"))
                .isInstanceOf(FriendshipNotAcceptedException.class);

        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void deletePendingRequestDeletesPendingFriendship() {
        Friendship friendship = new Friendship(alice, bob, alice);
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.of(friendship));

        friendshipService.deletePendingRequest(bob.getId(), "alice");

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    void deletePendingRequestRejectsAcceptedFriendship() {
        Friendship friendship = acceptedFriendship(alice, bob, alice, bob);
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendshipService.deletePendingRequest(alice.getId(), "bob"))
                .isInstanceOf(FriendshipNotPendingException.class);

        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void userOrderIsNormalizedBeforeLookupWhenDeletingFriendship() {
        Friendship friendship = acceptedFriendship(alice, bob, alice, bob);
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendshipRepository.findByUser1AndUser2(alice, bob)).thenReturn(Optional.of(friendship));

        friendshipService.deleteFriend(bob.getId(), "alice");

        FriendshipPair normalized = Friendship.normalizeUsers(bob, alice);
        verify(friendshipRepository).findByUser1AndUser2(normalized.firstUser(), normalized.secondUser());
        verify(friendshipRepository).delete(friendship);
    }

    private static Friendship acceptedFriendship(User requester, User receiver, User first, User second) {
        Friendship friendship = new Friendship(first, second, requester);
        friendship.acceptFriendship(receiver);
        return friendship;
    }

    private static User user(String id, String username) {
        return new User(UUID.fromString(id), username, username, "hash", "UTC");
    }
}
