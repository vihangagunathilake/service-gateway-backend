package com.flex.job_module.api.services;

import com.flex.job_module.api.http.requests.PointJobs;
import com.flex.job_module.api.http.requests.PrepareJob;
import com.flex.job_module.api.http.requests.TransferJob;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface JobService {

    ResponseEntity<?> prepareJob(PrepareJob prepareJob, HttpServletRequest request);

    ResponseEntity<?> prepareJobV2(PrepareJob prepareJob, HttpServletRequest request);

    ResponseEntity<?> jobVerification(Integer id, HttpServletRequest request);

    ResponseEntity<?> removeDummyJob(Integer jobId, Integer customerId, HttpServletRequest request);

    ResponseEntity<?> pointWiseJobs(PointJobs pointJobs, HttpServletRequest request);

    ResponseEntity<?> dateWiseJobs(PointJobs pointJobs, HttpServletRequest request);

    ResponseEntity<?> allowToServe(Integer jobId, HttpServletRequest request);

    ResponseEntity<?> transferJob(TransferJob transferJob, HttpServletRequest request);
}
