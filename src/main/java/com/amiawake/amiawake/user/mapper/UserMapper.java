package com.amiawake.amiawake.user.mapper;

import com.amiawake.amiawake.user.dto.UserResponse;
import com.amiawake.amiawake.user.dto.UserSearchResponse;
import com.amiawake.amiawake.user.entity.User;

public class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getTimeZone(), user.getStatus());
    }

    public static UserSearchResponse toUserSearchResponse(User user) {
        return new UserSearchResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }
}
