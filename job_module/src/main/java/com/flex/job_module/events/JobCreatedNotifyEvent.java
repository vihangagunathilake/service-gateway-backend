package com.flex.job_module.events;

public record JobCreatedNotifyEvent(
        Integer jobId, Integer serviceCenterId, String serviceCenter
) {
}
