package com.amiawake.amiawake.userstate.mapper;

import com.amiawake.amiawake.userstate.dto.UserStateResponse;
import com.amiawake.amiawake.userstate.entity.UserState;

import java.util.Optional;

public final class UserStateMapper {
    private UserStateMapper() {
    }

    public static UserStateResponse toUserStateResponse(UserState userState) {
        return new UserStateResponse(
                userState.getSleepState(),
                userState.getConfidence(),
                Optional.ofNullable(userState.getCalculatedAt())
        );
    }
}
