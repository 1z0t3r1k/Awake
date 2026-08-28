package com.amiawake.amiawake.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisplayNameRequest(@NotBlank @Size(max = 32) String displayName) {
}
