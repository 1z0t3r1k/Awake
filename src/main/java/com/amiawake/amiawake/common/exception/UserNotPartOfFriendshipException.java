package com.amiawake.amiawake.common.exception;

public class UserNotPartOfFriendshipException extends RuntimeException {

    public UserNotPartOfFriendshipException() {
        super("User is not part of this friendship");
    }
}