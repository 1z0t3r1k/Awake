package com.amiawake.amiawake.common.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("You can no longer use this token.");
    }
}
