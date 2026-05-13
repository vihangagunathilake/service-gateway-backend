package com.flex.dashboard_module.api.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface DashboardService {

    ResponseEntity<?> dailyDashboard(HttpServletRequest request);
}
