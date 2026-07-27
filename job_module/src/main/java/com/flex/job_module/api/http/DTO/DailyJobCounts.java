package com.flex.job_module.api.http.DTO;

public interface DailyJobCounts {
    Long getTotalJobs();

    Long getPending();

    Long getServing();

    Long getCompleted();

    Long getOnGoing();

    Long getTransferred();

    Long getTotalEarnings();
}
