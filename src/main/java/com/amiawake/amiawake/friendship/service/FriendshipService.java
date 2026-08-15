package com.amiawake.amiawake.friendship.service;

import com.amiawake.amiawake.common.exception.CannotAcceptOwnFriendRequestException;
import com.amiawake.amiawake.common.exception.CannotFriendYourselfException;
import com.amiawake.amiawake.common.exception.FriendRequestNotFoundException;
import com.amiawake.amiawake.common.exception.FriendshipAlreadyExistsException;
import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.friendship.entity.Friendship;
import com.amiawake.amiawake.friendship.entity.FriendshipPair;
import com.amiawake.amiawake.friendship.repository.FriendshipRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    public void sendFriendRequest(UUID requesterId, String username) {
        User receiver = userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
        User requester = userRepository.findById(requesterId).orElseThrow(() -> new UserNotFoundException(requesterId));

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
        User acceptor = userRepository.findById(acceptorId).orElseThrow(() -> new UserNotFoundException(acceptorId));
        User requester = userRepository.findByUsername(requesterUsername).orElseThrow(() -> new UserNotFoundException(requesterUsername));

        if (acceptorId.equals(requester.getId())) {
            throw new CannotAcceptOwnFriendRequestException();
        }

        FriendshipPair friendshipPair = Friendship.normalizeUsers(acceptor, requester);

        Friendship friendship = friendshipRepository.findByUser1AndUser2(friendshipPair.firstUser(), friendshipPair.secondUser())
                .orElseThrow(FriendRequestNotFoundException::new);

        friendship.acceptFriendship(acceptor);
    }
}
