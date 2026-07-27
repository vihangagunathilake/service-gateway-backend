package com.flex.job_module.api.http.DTO;

import java.time.LocalTime;

public interface JobAtPointDetails {
    String getPointName();

    String getServices();

    Integer getDownPayment();

    LocalTime getExpectedStartTime();

    LocalTime getExpectedEndTime();

    LocalTime getStartedTime();

    LocalTime getEndTime();

    String getAgent();

    Integer getStatus();
}
