package com.flex.user_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentNextJob {
    private Integer jobId;
    private String customerMobile;
    private String customer;
    private List<String> services;
    private String startTime;
    private String endTime;
    private String startedTime;
    private String endedTime;
    private int status;

}
