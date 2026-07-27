package com.flex.job_module.api.http.responses;

import com.flex.job_module.impl.entities.JobAtPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreparedJobV2 {
    private Integer jobId;
    private Integer customerId;
    private String appointmentDate;
    private String appointmentTime;
    private List<NewJobsAtPoint> jobsAtPoint;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewJobsAtPoint {
        private Integer id;
        private String service;
        private String servicePoint;
        private String startTime;
        private String endTime;
        private String customer;
    }
}
