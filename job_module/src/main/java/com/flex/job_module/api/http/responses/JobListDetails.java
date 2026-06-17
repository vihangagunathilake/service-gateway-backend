package com.flex.job_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobListDetails {
    private Integer jobId;
    private String customerName;
    private String service;
    private List<String> services;
    private List<String> points;
    private String timeSlot;
    private Integer status;
}
