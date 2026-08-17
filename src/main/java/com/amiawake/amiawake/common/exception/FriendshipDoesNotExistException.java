package com.amiawake.amiawake.common.exception;

public class FriendshipDoesNotExistException extends RuntimeException {

    public FriendshipDoesNotExistException() {
        super("Friendship does not exist");
    }
}