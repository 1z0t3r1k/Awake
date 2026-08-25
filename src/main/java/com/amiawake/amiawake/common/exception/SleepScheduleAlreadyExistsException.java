package com.amiawake.amiawake.common.exception;

public class SleepScheduleAlreadyExistsException extends RuntimeException {

    public SleepScheduleAlreadyExistsException() {
        super("Sleep schedule already exists");
    }
}