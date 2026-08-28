package com.amiawake.amiawake.user.dto;

import java.util.UUID;

public record UserSearchResponse(UUID userId, String username, String displayName) {
}
