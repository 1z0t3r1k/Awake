package com.amiawake.amiawake.common.exception;

public class CannotFriendYourselfException extends RuntimeException {
    public CannotFriendYourselfException() {
        super("You cannot send a friend request to yourself");
    }
}
