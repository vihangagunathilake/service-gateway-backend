package com.flex.user_module.api.services;

import com.flex.user_module.api.http.requests.AgentServing;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface AgentService {
    ResponseEntity<?> authenticate(HttpServletRequest request);

    ResponseEntity<?> agentLogin(Integer pointId, Integer userId, HttpServletRequest request);

    ResponseEntity<?> agentJobs(Integer servicePointId, HttpServletRequest request);

    ResponseEntity<?> servingJob(AgentServing agentServing, HttpServletRequest request);

    ResponseEntity<?> agentJobsInfo(Integer pointId, HttpServletRequest request);

    ResponseEntity<?> agentJobsRecords(LocalDate date, HttpServletRequest request);
}
