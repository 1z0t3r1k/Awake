package com.amiawake.amiawake.user.dto;

import java.util.UUID;

public record UserResponse(UUID id, String username, String displayName, String timeZone) {
}
