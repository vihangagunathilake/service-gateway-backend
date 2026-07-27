package com.flex.job_module.impl.services.algorithm.dto;

import com.flex.service_module.impl.entities.ServicePoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewJobAtPoint {
    private LocalTime nextStartTime;
    private Integer totalPayment;
    private Integer downPayment;
}
