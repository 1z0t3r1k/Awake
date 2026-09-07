package com.amiawake.amiawake.userstate.service;

import com.amiawake.amiawake.common.exception.FriendStateAccessDeniedException;
import com.amiawake.amiawake.friendship.entity.Friendship;
import com.amiawake.amiawake.friendship.entity.FriendshipPair;
import com.amiawake.amiawake.friendship.entity.FriendshipStatus;
import com.amiawake.amiawake.friendship.repository.FriendshipRepository;
import com.amiawake.amiawake.inference.model.InferenceResult;
import com.amiawake.amiawake.inference.states.SleepState;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.userstate.dto.UserStateResponse;
import com.amiawake.amiawake.userstate.entity.UserState;
import com.amiawake.amiawake.userstate.mapper.UserStateMapper;
import com.amiawake.amiawake.userstate.repository.UserStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserStateService {
    private final UserStateRepository userStateRepository;
    private final UserService userService;
    private final FriendshipRepository friendshipRepository;

    public UserStateService(
            UserStateRepository userStateRepository, UserService userService,
            FriendshipRepository friendshipRepository
    ) {
        this.userStateRepository = userStateRepository;
        this.userService = userService;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public void upsertUserState(User user, InferenceResult inferenceResult) {
        Optional<UserState> optionalUserState = userStateRepository.findById(user.getId());

        if (optionalUserState.isEmpty()) {
            userStateRepository.save(new UserState(user, inferenceResult.state(), inferenceResult.confidence()));
        } else {
            UserState userState = optionalUserState.get();
            userState.updateState(inferenceResult.state(), inferenceResult.confidence());
        }
    }

    public UserStateResponse getUserState(User user) {
        Optional<UserState> optionalUserState =
                userStateRepository.findById(user.getId());

        return optionalUserState.map(UserStateMapper::toUserStateResponse).orElseGet(() -> new UserStateResponse(
                SleepState.UNKNOWN,
                0.0,
                Optional.empty()
        ));
    }

    public UserStateResponse getFriendState(UUID userId, String username) {
        User user = userService.getUserById(userId);
        User friend = userService.getUserByUsername(username);

        FriendshipPair friendshipPair = Friendship.normalizeUsers(user, friend);

        Friendship friendship = friendshipRepository.findByUser1AndUser2(
                friendshipPair.firstUser(),
                friendshipPair.secondUser()
        ).orElseThrow(FriendStateAccessDeniedException::new);

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new FriendStateAccessDeniedException();
        }

        return getUserState(friend);
    }
}
