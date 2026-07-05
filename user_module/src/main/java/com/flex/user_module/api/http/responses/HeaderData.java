package com.flex.user_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/24/2026
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HeaderData {
    private String userType;
    private String email;
    private String providerId;
    private String userName;
    private String serviceCenter;
    private String image;
    private String loggedInPoint;
    private Integer loggedInPointId;
}
