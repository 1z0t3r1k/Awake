package com.amiawake.amiawake.common.exception;

public class FriendshipAlreadyExistsException extends RuntimeException {
    public FriendshipAlreadyExistsException() {
        super("Friendship already exists");
    }
}
