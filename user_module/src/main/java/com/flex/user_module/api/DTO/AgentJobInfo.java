package com.flex.user_module.api.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AgentJobInfo {
    LocalDate getAddedDate();

    String getCustomer();

    String getCustomerMobile();

    String getJobId();

    String getService();

    String getServicePointName();

    String getStartedTime();

    String getEndedTime();

    LocalTime getExpectedStartTime();

    LocalTime getExpectedEndTime();

    Long getDuration();

    Long getExpectedDuration();

    Double getDurationRate();
}
