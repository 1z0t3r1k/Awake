package com.amiawake.amiawake.friendship.dto;

import com.amiawake.amiawake.user.entity.User.AvailabilityStatus;

public record FriendResponse(String username, AvailabilityStatus status) {
}
