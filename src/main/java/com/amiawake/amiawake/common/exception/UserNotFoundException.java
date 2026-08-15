package com.amiawake.amiawake.common.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("User with id " + id + " not found");
    }

    public UserNotFoundException(String username) {
        super("User with name " + username + " not found");
    }
}