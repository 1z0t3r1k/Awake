package com.amiawake.amiawake.common.exception;

public class InvalidSleepScheduleException extends RuntimeException {

    public InvalidSleepScheduleException() {
        super("Sleep time and wake time must be different");
    }
}