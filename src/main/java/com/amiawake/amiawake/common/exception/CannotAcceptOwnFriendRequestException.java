package com.amiawake.amiawake.common.exception;

public class CannotAcceptOwnFriendRequestException extends RuntimeException {
    public CannotAcceptOwnFriendRequestException() {
        super("Cannot accept your own friend request");
    }
}
