package com.amiawake.amiawake.common.exception;

public class CannotSubscribeToSelfException extends RuntimeException {

    public CannotSubscribeToSelfException() {
        super("User cannot subscribe to their own wake-up");
    }
}
