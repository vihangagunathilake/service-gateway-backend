package com.flex.user_module.impl.services;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.repositories.ServiceCenterRepository;
import com.flex.service_module.impl.repositories.ServicePointRepository;
import com.flex.user_module.api.http.requests.AgentServing;
import com.flex.user_module.api.http.responses.AgentDetails;
import com.flex.user_module.api.http.responses.AgentJobInfo;
import com.flex.user_module.api.http.responses.AgentNextJob;
import com.flex.user_module.api.services.AgentService;
import com.flex.user_module.events.AgentLoginEvent;
import com.flex.user_module.events.JobServingEvent;
import com.flex.user_module.events.RoleCreatedEvent;
import com.flex.user_module.impl.entities.AgentJob;
import com.flex.user_module.impl.entities.AgentLogin;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.AgentJobRepository;
import com.flex.user_module.impl.repositories.AgentLoginRepository;
import com.flex.user_module.impl.repositories.UserRepository;
import com.flex.user_module.impl.services.helpers.AgentServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.flex.common_module.CommonMethods.*;
import static com.flex.common_module.http.ReturnResponse.*;
import static com.flex.job_module.constants.JobStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class AgentServiceImpl implements AgentService {

    private final UserRepository userRepository;
    private final AgentLoginRepository agentLoginRepository;
    private final AgentJobRepository agentJobRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePointRepository servicePointRepository;
    private final JobAtPointRepository jobAtPointRepository;
    private final JobRepository jobRepository;

    private final AgentServiceHelper agentServiceHelper;

    private final ApplicationEventPublisher publisher;

    @Override
    public ResponseEntity<?> authenticate(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("Not allowed for this action");
        }

        Integer userId = userClaims.getUserId();

        User user = userRepository.findByIdAndDeletedIsFalse(userId);

        if (user == null) {
            return CONFLICT("User not found");
        }

        Integer serviceCenterId = userClaims.getCenter();

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(serviceCenterId);

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        return DATA(AgentDetails.builder()
                .agentId(user.getId())
                .agentName(user.getFName())
                .agentEmail(user.getEmail())
                .centerId(serviceCenter.getId())
                .centerName(serviceCenter.getName())
                .build());
    }

    @Override
    public ResponseEntity<?> agentLogin(Integer pointId, Integer userId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(pointId);

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        // another agent is login at the same point at same time, do not let this agent to login to this point
        AgentLogin anotherAgentInSamePoint = agentLoginRepository.getUserLoginToPoint(pointId, userId);

        if (anotherAgentInSamePoint != null) {
            return CONFLICT(anotherAgentInSamePoint.getUser().getFName()
                    + " already logged in "
                    + anotherAgentInSamePoint.getServicePoint().getName());
        }

        // this agent somehow close the browser or whatever reason, out from the system while he is serving.
        // now that same agent when try to login to the system again, that agent should not be able to login to another point.
        // he must login to the previous point and end the job.
        // therefore do not let this agent to login to this point.
        List<AgentJob> agentServingJobInAnotherPoint = agentJobRepository
                .getServingJobByAgentAndNotInPoint(userId, pointId, CommonMethods.getCurrentDate());

        if (!agentServingJobInAnotherPoint.isEmpty()) {
            return CONFLICT("You already serving a job at "
                    + agentServingJobInAnotherPoint.getFirst().getServicePoint().getName()
                    + ". Please login to that point");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userId);

        if (user == null) {
            return CONFLICT("User not found");
        }

        List<AgentLogin> previousLogins = agentLoginRepository.getAgentLogins(userId);

        for (AgentLogin agentLogin : previousLogins) {
            agentLogin.setLogoutDate(getCurrentDate());
            agentLogin.setLogoutTime(getCurrentTime());

            agentLoginRepository.save(agentLogin);
        }

        AgentLogin agentLogin = AgentLogin.builder()
                .user(user)
                .servicePoint(servicePoint)
                .loginDate(getCurrentDate())
                .loginTime(getCurrentTime())
                .build();

        agentLoginRepository.save(agentLogin);

        return SUCCESS("");
    }

    @Override
    public ResponseEntity<?> agentJobs(Integer servicePointId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(servicePointId);

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        List<Integer> jobsIds = jobAtPointRepository.getJobsIdsInPoint(servicePointId,
                getCurrentDate());

        log.info("ids: {}", jobsIds);

        if (jobsIds.isEmpty()) {
            log.warn("jobs not found in {}", servicePointId);
            return DATA(null);
        }
        //get first pending job because this has order.

        //check if a service which is before this service is already in serving.
        //if true do not show this service.
        //ex: body wash in serving in different bay. and we can see next services of the same job in different bay.
        // so we can start serving it. now status is bay 1 job 1 body wash serving, bay 1 job 1 other services serving
        //it can't be
        List<AgentJob> currentlyServingJobs = agentJobRepository
                .getServingJobsAtOtherPointsRelatedToJob(jobsIds.getFirst(), servicePointId,
                        getCurrentDate());

        if (!currentlyServingJobs.isEmpty()) {
            return DATA(null);
        }

        List<JobAtPoint> jobAtPoints = jobAtPointRepository.getPendingJobsAtPointByPointAndJob(
                servicePointId, jobsIds.getFirst(), getCurrentDate()
        );

        if (jobAtPoints.isEmpty()) {
            log.warn("jobs at point not found for {}", jobsIds.getFirst());
            return DATA(null);
        }

        // need started time
        // need ended time
        AgentNextJob agentNextJob = AgentNextJob.builder().build();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

        Integer jobId = null;
        String customerMobile  = null;
        String customer  = null;
        List<String> services  = new ArrayList<>();
        LocalTime startTime  = null;
        LocalTime endTime  = null;
        String startedTime  = null;
        String endedTime  = null;
        int status = JobStatus.PENDING;

        for (JobAtPoint jobAtPoint : jobAtPoints) {
            if (jobId == null) {
                jobId = jobAtPoint.getJob().getId();
            }

            if (customerMobile == null) {
                customerMobile = jobAtPoint.getJob().getCustomer().getPhone();
            }

            if (customer == null) {
                customer = jobAtPoint.getJob().getCustomer().getCustomer();
            }

            if (startTime == null) {
                startTime = jobAtPoint.getStartTime();
            } else {
                if (startTime.isAfter(jobAtPoint.getStartTime())) {
                    startTime = jobAtPoint.getStartTime();
                }
            }

            if (endTime == null) {
                endTime = jobAtPoint.getEndTime();
            } else {
                if (endTime.isBefore(jobAtPoint.getEndTime())) {
                    endTime = jobAtPoint.getEndTime();
                }
            }

            AgentJob agentJobs = agentJobRepository
                    .findByServicePoint_idAndJobAtPoint_id(servicePoint.getId(), jobAtPoint.getId());

            if (agentJobs != null) {
                startedTime = agentJobs.getStartTime().format(formatter);
            }

            if (agentJobs != null && agentJobs.getEndTime() != null) {
                endedTime = agentJobs.getEndTime().format(formatter);
            }

            if (jobAtPoint.getStatus() == IN_SERVICE) {
                status = IN_SERVICE;
            }

            if (jobAtPoint.getStatus() == COMPLETED) {
                status = COMPLETED;
            }

            services.add(jobAtPoint.getService().getName());

            agentNextJob.setJobId(jobId);
            agentNextJob.setCustomerMobile(customerMobile);
            agentNextJob.setCustomer(customer);
            agentNextJob.setStartTime(startTime.format(formatter));
            agentNextJob.setEndTime(endTime.format(formatter));
            agentNextJob.setStartedTime(startedTime);
            agentNextJob.setEndedTime(endedTime);
            agentNextJob.setStatus(status);
            agentNextJob.setServices(services);
        }

        return DATA(agentNextJob);
    }

    //todo this service may change according to settings or configuration
    // this is by default serving for all services at once. But there can be single jobs at point serving as well.
    // so prepare for that
    @Transactional
    @Override
    public ResponseEntity<?> servingJob(AgentServing agentServing, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(agentServing.getServicePointId());

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        Job job = jobRepository.getJobById(agentServing.getJobId());

        if (job == null) {
            log.info("job id: {}", agentServing.getJobId());
            return CONFLICT("Job not found");
        }

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User agent = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (agent == null) {
            return CONFLICT("User not found");
        }

        List<AgentJob> currentlyServingJobs = agentJobRepository.getAgentServingJobs(
                agent.getId(), getCurrentDate()
        );

        if (!currentlyServingJobs.isEmpty() && currentlyServingJobs.size() > 1) {
            return CONFLICT("Serving job already exists in " + currentlyServingJobs.getFirst().getServicePoint().getName());
        }

        List<JobAtPoint> jobAtPoints = jobAtPointRepository
                .findByServicePointAndJobIdAndAppointmentDate(servicePoint.getId(), agentServing.getJobId(),
                        getCurrentDate());

        int status = PENDING;
        int jobStatus;

        for (JobAtPoint jobAtPoint : jobAtPoints) {
            if (jobAtPoint.getStatus() == PENDING) {
                status = IN_SERVICE;
                jobStatus = status;
                job.setStatus(jobStatus);
                jobAtPoint.setStatus(status);
                jobRepository.save(job);
                jobAtPointRepository.save(jobAtPoint);
                agentServiceHelper.servingJob(jobAtPoint, agent, status);

                publisher.publishEvent(
                        new AgentLoginEvent(
                                servicePoint,
                                job
                        )
                );

            } else if (jobAtPoint.getStatus() == IN_SERVICE) {
                status = COMPLETED;
                jobStatus = status;

                // first job at point may completed. But there are some other pending job at points.
                List<Integer> pendingJobAtPointsIds = jobAtPointRepository.getPendingJobsAtPointIdsByJobId(
                        job.getId(), job.getAppointmentDate()
                );

                if (!pendingJobAtPointsIds.isEmpty()) {
                    jobStatus = ON_GOING;
                }

                job.setStatus(jobStatus);
                jobAtPoint.setStatus(status);
                jobRepository.save(job);
                jobAtPointRepository.save(jobAtPoint);
                agentServiceHelper.servingJob(jobAtPoint, agent, status);
            }
        }

        String responseMessage = null;

        if (status == IN_SERVICE) {

            agentServiceHelper.markTheAgentLogin(job.getId(), agent);

            responseMessage = job.getId() + " in serving";
            agentServiceHelper.markTheTokenInServing(job.getId(), agent, servicePoint.getName());
        } else if (status == COMPLETED) {
            responseMessage = job.getId() + " completed";
            agentServiceHelper.markTheTokenCompleted(job.getId(), agent, servicePoint.getName());
        }

        publisher.publishEvent(
                new JobServingEvent(
                        servicePoint.getServiceCenter().getServiceProvider().getId()
                )
        );

        return SUCCESS(responseMessage);
    }

    @Override
    public ResponseEntity<?> agentJobsInfo(Integer pointId, HttpServletRequest request) {
        log.info(request.getRequestURI());
        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("Not allowed for this action");
        }

        Integer userId = userClaims.getUserId();

        User user = userRepository.findByIdAndDeletedIsFalse(userId);

        if (user == null) {
            return CONFLICT("User not found");
        }

        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(pointId);

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        // get pending jobs for count
        List<Integer> pendingJobIds = jobAtPointRepository.getOnlyPendingJobAtPointIdsByPoint(servicePoint.getId(),
                getCurrentDate());

        // get total completed jobs
        List<AgentJob> agentCompletedJobs = agentJobRepository.getAgentCompletedJobs(user.getId(),
                getCurrentDate());

        // get job completing rate.
        List<AgentJob> agentSuccessfullyCompletedJobs = agentJobRepository.getAgentSuccessfullyCompletedJobs(user.getId(),
                getCurrentDate());

        int completedJobs = agentCompletedJobs.size();
        int pending = pendingJobIds.size();
        int successfullyCompletedJobs = agentSuccessfullyCompletedJobs.size();

        return DATA(
                AgentJobInfo.builder()
                        .pending(pending)
                        .completed(completedJobs)
                        .completedRate(
                                completedJobs == 0
                                        ? 0
                                        : Math.round(((float) successfullyCompletedJobs / completedJobs) * 100)
                        )
                        .build()
        );
    }

    @Override
    public ResponseEntity<?> agentJobsRecords(LocalDate date, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("Not allowed for this action");
        }

        Integer userId = userClaims.getUserId();

        User user = userRepository.findByIdAndDeletedIsFalse(userId);

        if (user == null) {
            return CONFLICT("User not found");
        }

        return DATA(agentJobRepository.findAgentJobs(date));
    }
}
