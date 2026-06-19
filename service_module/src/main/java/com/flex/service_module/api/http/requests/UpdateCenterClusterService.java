package com.flex.service_module.api.http.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCenterClusterService {

    private Integer id;
    private Integer total;
    private String service;
    private Integer downPay;
    private Integer orderNumber;

    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Colombo")
    private LocalTime serviceTime;

    private Boolean disabled;

}
