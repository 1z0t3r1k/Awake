package com.amiawake.amiawake.common.dto;

import java.util.Map;

public record ApiErrorResponse(int status, String message, Map<String, String> errors) {
}
