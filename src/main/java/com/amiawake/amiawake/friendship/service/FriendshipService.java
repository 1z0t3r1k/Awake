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
import com.amiawake.amiawake.friendship.mapper.FriendshipMapper;
import com.amiawake.amiawake.friendship.repository.FriendshipRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.amiawake.amiawake.userstate.dto.UserStateResponse;
import com.amiawake.amiawake.userstate.service.UserStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserStateService userStateService;

    public FriendshipService(
            FriendshipRepository friendshipRepository, UserRepository userRepository, UserStateService userStateService
    ) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.userStateService = userStateService;
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    private Friendship getFriendshipByUsers(User user1, User user2) {
        FriendshipPair friendshipPair = Friendship.normalizeUsers(user1, user2);

        return friendshipRepository
                .findByUser1AndUser2(friendshipPair.firstUser(), friendshipPair.secondUser())
                .orElseThrow(FriendshipDoesNotExistException::new);
    }

    public void sendFriendRequest(UUID requesterId, String username) {
        User receiver = getUserByUsername(username);
        User requester = getUserById(requesterId);

        if (receiver.getId().equals(requester.getId())) {
            throw new CannotFriendYourselfException();
        }

        Friendship friendship = new Friendship(
                requester,
                receiver,
                requester
        );

        User user1 = friendship.getUser1();
        User user2 = friendship.getUser2();
        if (friendshipRepository.findByUser1AndUser2(user1, user2).isPresent()) {
            throw new FriendshipAlreadyExistsException();
        }

        friendshipRepository.save(friendship);
    }

    @Transactional
    public void acceptFriendRequest(UUID acceptorId, String requesterUsername) {
        User acceptor = getUserById(acceptorId);
        User requester = getUserByUsername(requesterUsername);

        if (acceptorId.equals(requester.getId())) {
            throw new CannotAcceptOwnFriendRequestException();
        }

        Friendship friendship = getFriendshipByUsers(acceptor, requester);

        friendship.acceptFriendship(acceptor);
    }

    public List<IncomingFriendRequestResponse> getIncomingFriendRequests(UUID userId) {
        User user = getUserById(userId);
        List<Friendship> friendships = friendshipRepository.findIncomingRequests(user, FriendshipStatus.PENDING);

        List<IncomingFriendRequestResponse> friendRequests = new ArrayList<>(friendships.size());

        for (Friendship friendship : friendships) {
            friendRequests.add(FriendshipMapper.toIncomingRequestResponse(friendship));
        }

        return friendRequests;
    }

    public List<FriendResponse> getFriends(UUID userId) {
        User user = getUserById(userId);

        List<Friendship> friendships =
                friendshipRepository.findFriendships(
                        user,
                        FriendshipStatus.ACCEPTED
                );

        List<FriendResponse> friendResponses =
                new ArrayList<>(friendships.size());

        for (Friendship friendship : friendships) {
            User friend = friendship.getOtherUser(user);

            UserStateResponse friendState =
                    userStateService.getUserState(friend);

            friendResponses.add(
                    FriendshipMapper.toFriendResponse(
                            friendship,
                            user,
                            friendState
                    )
            );
        }

        return friendResponses;
    }

    @Transactional
    public void deleteFriend(UUID userId, String username) {
        User user = getUserById(userId);
        User friendToDelete = getUserByUsername(username);

        Friendship friendship = getFriendshipByUsers(user, friendToDelete);

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new FriendshipNotAcceptedException();
        }

        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void deletePendingRequest(UUID userId, String username) {
        User user = getUserById(userId);
        User otherUser = getUserByUsername(username);

        Friendship friendship = getFriendshipByUsers(user, otherUser);

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipNotPendingException();
        }

        friendshipRepository.delete(friendship);
    }

    public List<OutgoingFriendRequestResponse> getOutgoingFriendRequests(UUID userId) {
        User user = getUserById(userId);
        List<Friendship> friendships = friendshipRepository.findOutgoingRequests(user, FriendshipStatus.PENDING);

        List<OutgoingFriendRequestResponse> friendRequests = new ArrayList<>(friendships.size());

        for (Friendship friendship : friendships) {
            friendRequests.add(FriendshipMapper.toOutgoingRequestResponse(friendship, user));
        }

        return friendRequests;
    }
}
