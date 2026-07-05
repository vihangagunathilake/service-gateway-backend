package com.flex.user_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentServingJob {
    private String jobId;
    private String customerMobile;
    private LocalTime startTime;
    private LocalTime endTime;
    private String services;
}
