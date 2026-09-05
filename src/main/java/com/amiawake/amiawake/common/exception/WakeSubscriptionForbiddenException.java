package com.amiawake.amiawake.common.exception;

public class WakeSubscriptionForbiddenException extends RuntimeException {

    public WakeSubscriptionForbiddenException() {
        super("Wake subscription is allowed only for accepted friends");
    }
}
