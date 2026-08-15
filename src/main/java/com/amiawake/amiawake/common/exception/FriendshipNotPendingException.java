package com.amiawake.amiawake.common.exception;

public class FriendshipNotPendingException extends RuntimeException {

    public FriendshipNotPendingException() {
        super("Friend request is not pending");
    }
}