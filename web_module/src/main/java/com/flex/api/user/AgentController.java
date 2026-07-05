package com.flex.api.user;

import com.flex.user_module.api.http.requests.AgentServing;
import com.flex.user_module.api.services.AgentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/authentication")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> systemPermissions(HttpServletRequest request) {
        return agentService.authenticate(request);
    }

    @GetMapping("/login/user/{userId}/to-point/{pointId}")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> agentLogin(@PathVariable Integer userId,
                                        @PathVariable Integer pointId,
                                        HttpServletRequest request) {
        return agentService.agentLogin(pointId, userId, request);
    }

    @GetMapping("/points/{pointId}/jobs")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).EJ)")
    public ResponseEntity<?> agentJobs(@PathVariable Integer pointId,
                                        HttpServletRequest request) {
        return agentService.agentJobs(pointId, request);
    }

    @PostMapping("/serving")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).EJ)")
    public ResponseEntity<?> servingJob(@RequestBody AgentServing agentServing,
                                        HttpServletRequest request) {
        return agentService.servingJob(agentServing, request);
    }

    @GetMapping("/point/{pointId}/jobs-info")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).EJ)")
    public ResponseEntity<?> agentJobsInfo(@PathVariable Integer pointId,
                                        HttpServletRequest request) {
        return agentService.agentJobsInfo(pointId, request);
    }

    @GetMapping("/records/date/{date}")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).EJ)")
    public ResponseEntity<?> agentRecords(@PathVariable LocalDate date,
                                           HttpServletRequest request) {
        return agentService.agentJobsRecords(date, request);
    }
}
