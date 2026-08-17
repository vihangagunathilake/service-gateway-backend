package com.flex.dashboard_module.impl.services;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.dashboard_module.api.http.response.DashboardData;
import com.flex.dashboard_module.api.services.DashboardService;
import com.flex.job_module.api.http.DTO.CenterJob;
import com.flex.job_module.api.http.DTO.DailyJobCounts;
import com.flex.job_module.api.http.DTO.classes.ClusterWiseDownPayments;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.service_module.impl.entities.Cluster;
import com.flex.service_module.impl.repositories.ClusterRepository;
import com.flex.service_module.impl.repositories.ServiceCenterRepository;
import com.flex.service_module.impl.repositories.ServicePointRepository;
import com.flex.user_module.constants.UserConstant;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.AgentLoginRepository;
import com.flex.user_module.impl.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flex.common_module.http.ReturnResponse.*;
import static com.flex.job_module.constants.JobStatus.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 5/11/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ServicePointRepository servicePointRepository;
    private final AgentLoginRepository agentLoginRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final JobAtPointRepository jobAtPointRepository;
    private final ClusterRepository clusterRepository;

    @Override
    public ResponseEntity<?> dailyDashboard(HttpServletRequest request) {

        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        Integer userId = userClaims.getUserId();

        User user = userRepository.findByIdAndDeletedIsFalse(userId);

        if (user == null) {
            return CONFLICT("User not found");
        }

        if (user.getServiceProvider().isDeleted()) {
            return CONFLICT("No access to this resource");
        }

        LocalDate today = CommonMethods.getCurrentDate();

        DashboardData dashboardData = DashboardData.builder().build();

        //------ jobs count summary
        DailyJobCounts dailyJobCounts = jobRepository.getJobSummary(user.getServiceProvider().getId(), today,
                PENDING, IN_SERVICE, COMPLETED, ON_GOING, CANCEL, TIMEOUT, TRANSFER);

        dashboardData.setTotalJobs(dailyJobCounts == null || dailyJobCounts.getTotalJobs() == null ?
                0 : dailyJobCounts.getTotalJobs());
        dashboardData.setPendingJobs(dailyJobCounts == null || dailyJobCounts.getPending() == null ?
                0 : dailyJobCounts.getPending());
        dashboardData.setServingJobs(dailyJobCounts == null || dailyJobCounts.getServing() == null ?
                0 : dailyJobCounts.getServing());
        dashboardData.setCompletedJobs(dailyJobCounts == null || dailyJobCounts.getCompleted() == null ?
                0 : dailyJobCounts.getCompleted());
        dashboardData.setTotalEarnings(dailyJobCounts == null || dailyJobCounts.getTotalEarnings() == null ?
                0 : dailyJobCounts.getTotalEarnings());

        //------ timeout jobs
        List<Integer> timeoutJobIds = jobRepository
                .getJobIdsByStatusAndProvider(TIMEOUT, user.getServiceProvider().getId());

        List<JobAtPoint> jobAtPoints = jobAtPointRepository.getPricesOfJobsByJobIds(timeoutJobIds);

        int totalDownPayment = 0;
        int totalPayment = 0;

        for (JobAtPoint jobAtPoint : jobAtPoints) {
            totalDownPayment = totalDownPayment + jobAtPoint.getServiceDownPrice();
            totalPayment = totalPayment + jobAtPoint.getServiceTotalPrice();
        }


        Long transferred = dailyJobCounts == null || dailyJobCounts.getTransferred() == null ?
                0 : dailyJobCounts.getTransferred();

        dashboardData.setTimeoutJobs(DashboardData.TimeoutJobs.builder()
                .count(timeoutJobIds.size())
                .alreadyPaid(totalDownPayment)
                .pendingPaid(totalPayment - totalDownPayment)
                .rescheduleCount(transferred)
                .build());

        //------ operational centers
        int totalCenters = serviceCenterRepository.getCountByServiceProvider(user.getServiceProvider().getId());

        int totalUsers = userRepository.getCountByServiceProviderAndDeletedIsFalse(user.getServiceProvider().getId(),
                UserConstant.EMPLOYEE);

        int totalPoints = servicePointRepository.getCountByServiceProviderId(user.getServiceProvider().getId());

        int activeCenters = agentLoginRepository.getActiveCentersByServiceProvider(user.getServiceProvider().getId());


        //todo: in future may be multiple agents can login to the same point. in that case this will change
        int agentLoginCount = agentLoginRepository.getAgentLoginCountByServiceProvider(user.getServiceProvider().getId());

        List<ClusterWiseDownPayments> clusterWiseDownPayments = new ArrayList<>();

        Map<String, Integer> servicesWithTime = new HashMap<>();

        List<Job> jobList = jobRepository
                .getAllJobsByProviderId(user.getServiceProvider().getId());

        for (Job job : jobList) {

            if (job.getClusterId() == null) {
                if (servicesWithTime.containsKey("Custom")) {
                    Integer downAmount = servicesWithTime.get("Custom");

                    downAmount = downAmount + job.getDownPayment();
                    servicesWithTime.put("Custom", downAmount);
                } else {
                    String service = "Custom";

                    servicesWithTime.put(service, job.getDownPayment());
                }
            } else {
                Cluster cluster = clusterRepository
                        .findByIdAndDeletedIsFalse(job.getClusterId());

                if (servicesWithTime.containsKey(cluster.getName())) {
                    Integer downAmount = servicesWithTime.get(cluster.getName());

                    downAmount = downAmount + job.getDownPayment();
                    servicesWithTime.put(cluster.getName(), downAmount);
                } else {
                    servicesWithTime.put(cluster.getName(), job.getDownPayment());
                }
            }
        }

        servicesWithTime.forEach((service, amount) -> {

            Integer jobCount;

            if (service.equals("Custom")) {
                jobCount = jobRepository.getCustomJobCountByCluster(COMPLETED);
            } else {
                Integer clusterId = clusterRepository.getClusterIdByName(service);

                jobCount = jobRepository.getJobCountByCluster(clusterId, COMPLETED);
            }

            ClusterWiseDownPayments clusterWiseDownPayment = ClusterWiseDownPayments.builder()
                    .service(service)
                    .amount(amount)
                    .jobCount(jobCount)
                    .build();

            clusterWiseDownPayments.add(clusterWiseDownPayment);
        });

        DashboardData.OperationalCenters operationalCenters = DashboardData.OperationalCenters.builder()
                .active(activeCenters)
                .total(totalCenters)
                .servicePoints(DashboardData.ServicePoints.builder().active(agentLoginCount).total(totalPoints).build())
                .employees(DashboardData.Employees.builder().loggedIn(agentLoginCount).total(totalUsers).build())
                .build();

        dashboardData.setOperationalCenters(operationalCenters);

        dashboardData.setDownpaymentData(clusterWiseDownPayments);

        //------- center wise bar chart
        List<CenterJob> centerJobs = jobRepository.getCenterJobsByProvider(user.getServiceProvider().getId(),
                CANCEL, TIMEOUT, TRANSFER, CommonMethods.getCurrentDate());

        List<DashboardData.CenterJobs> centerJobsList = centerJobs.stream().map(
                e -> DashboardData.CenterJobs.builder().name(e.getName()).jobs(e.getJobs()).build()
        ).toList();

        dashboardData.setCenterJobs(centerJobsList);

        return DATA(dashboardData);
    }
}
