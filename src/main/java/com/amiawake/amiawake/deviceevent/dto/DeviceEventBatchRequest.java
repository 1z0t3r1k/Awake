package com.amiawake.amiawake.deviceevent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeviceEventBatchRequest(@Size(max = 500) @NotEmpty List<@Valid DeviceEventRequest> events) {
}
