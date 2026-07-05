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
public class SubJobDetails {
    private String service;
    private String pointName;
    private String startTime;
    private String endTime;
    private String actualStartTime;
    private String actualEndTime;
    private String agent;
    private Integer status;
    private boolean completed;
    private boolean estimatedEndTime;
}
