package com.amiawake.amiawake.common.exception;

public class FriendStateAccessDeniedException extends RuntimeException {
    public FriendStateAccessDeniedException() {
        super("You don't have permission to view this user's state");
    }
}
