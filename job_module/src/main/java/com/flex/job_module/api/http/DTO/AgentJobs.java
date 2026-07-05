package com.flex.job_module.api.http.DTO;

import java.time.LocalTime;

public interface AgentJobs {
    Integer getJobId();

    String getCustomerMobile();

    String getCustomer();

    String getServices();

    String getStartTime();

    String getEndTime();

    String getStartedTime();

    String getEndedTime();

    Integer getStatus();
}
