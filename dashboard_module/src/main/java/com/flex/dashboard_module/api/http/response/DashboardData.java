package com.flex.dashboard_module.api.http.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardData {

    private Long totalJobs;
    private Long pendingJobs;
    private TimeoutJobs timeoutJobs;
    private OperationalCenters operationalCenters;
    private Long servingJobs;
    private Long completedJobs;
    private Long totalEarnings;
    private List<CenterJobs> centerJobs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeoutJobs {
        private Integer count;
        private Integer alreadyPaid;
        private Integer pendingPaid;
        private Integer rescheduleCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationalCenters {
        private Integer active;
        private Integer total;
        private ServicePoints servicePoints;
        private Employees employees;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicePoints {
        private Integer active;
        private Integer total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Employees {
        private Integer loggedIn;
        private Integer total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CenterJobs {
        private String name;
        private Integer jobs;
    }

}
