package com.flex.job_module.api.http.DTO.classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 8/15/2026
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClusterWiseDownPayments {

    private String service;
    private Integer amount;
    private Integer jobCount;

}
