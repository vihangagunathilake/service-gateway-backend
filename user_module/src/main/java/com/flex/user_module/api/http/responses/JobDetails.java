package com.flex.user_module.api.http.responses;

import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.entities.JobTrack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDetails {
    private Integer id;
    private String customer; //this is coming like 'Service 1, Service 2, Service 3'
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String serviceName;
    private List<String> pointName;
    private String centerName;

    private String status;

    private Double paidAmount;
    private Integer serviceFee;

    private String serviceTime;

    private String createdAt;

    private String appointmentMethod;

    private String appointmentDate;
    private String appointmentTime;

    private String description;

    private List<SubJobDetails> plan;
    private List<JobTrack> timeline;

    private boolean verifiedJob;

}

