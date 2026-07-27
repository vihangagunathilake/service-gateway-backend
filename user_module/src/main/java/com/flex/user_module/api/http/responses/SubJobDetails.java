package com.flex.user_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubJobDetails {
    private String pointName;
    private PointJobDetails pointJobDetails;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PointJobDetails {
        private List<String> services;
        private String startTime;
        private String endTime;
        private String actualStartTime;
        private String actualEndTime;
        private String agent;
        private Integer status;
    }
}
