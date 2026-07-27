package com.flex.job_module.api.http.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferJob {
    private Integer jobId;
    private Integer centerId;
    private LocalDate nextAppointmentDate;
}
