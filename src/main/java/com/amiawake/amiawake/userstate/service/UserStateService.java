package com.amiawake.amiawake.userstate.service;

import com.amiawake.amiawake.inference.model.InferenceResult;
import com.amiawake.amiawake.inference.states.SleepState;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.userstate.dto.UserStateResponse;
import com.amiawake.amiawake.userstate.entity.UserState;
import com.amiawake.amiawake.userstate.mapper.UserStateMapper;
import com.amiawake.amiawake.userstate.repository.UserStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserStateService {
    private final UserStateRepository userStateRepository;

    public UserStateService(UserStateRepository userStateRepository) {
        this.userStateRepository = userStateRepository;
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
}
