package com.amiawake.amiawake.common.exception;

public class WakeSubscriptionAlreadyExistsException extends RuntimeException {

    public WakeSubscriptionAlreadyExistsException() {
        super("Wake subscription already exists");
    }
}