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
public class JobDetails {
    private Integer id;
    private String customer;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String serviceName;
    private String pointName;
    private String centerName;

    private String status;

    private Double totalAmount;
    private Double paidAmount;
    private Integer serviceFee;

    private String serviceTime;

    private String createdAt;

    private String appointmentMethod;

    private String description;

    private List<SubJobDetails> timeline;

    private String customerArrivedTime;

    private boolean verifiedJob;
}
