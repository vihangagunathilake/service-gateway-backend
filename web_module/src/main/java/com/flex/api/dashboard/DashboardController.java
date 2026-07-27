package com.flex.api.dashboard;

import com.flex.dashboard_module.api.services.DashboardService;
import com.flex.job_module.api.http.requests.PrepareJob;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 7/18/2026
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/main")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> dashboardMain(HttpServletRequest request) {
        return dashboardService.dailyDashboard(request);
    }
}
