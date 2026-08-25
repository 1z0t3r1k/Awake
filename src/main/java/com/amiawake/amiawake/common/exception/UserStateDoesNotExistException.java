package com.amiawake.amiawake.common.exception;

public class UserStateDoesNotExistException extends RuntimeException {
    public UserStateDoesNotExistException() {
        super("User state does not exist");
    }
}
