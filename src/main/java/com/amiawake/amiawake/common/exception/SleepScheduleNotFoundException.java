package com.amiawake.amiawake.common.exception;

public class SleepScheduleNotFoundException extends RuntimeException {

    public SleepScheduleNotFoundException() {
        super("Sleep schedule not found");
    }
}