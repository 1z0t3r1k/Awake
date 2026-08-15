package com.amiawake.amiawake.friendship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FriendRequest(@NotBlank @NotNull String username) {
}
