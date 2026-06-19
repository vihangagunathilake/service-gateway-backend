package com.flex.service_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/1/2026
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SPProfile {
    private Integer id;
    private String name;
    private String regNo;
    private String email;
    private String contact;
    private String address;
    private String website;
    private String status;
    private String joinDate;
    private String description;
    private String profile;
    private String cover;
}
