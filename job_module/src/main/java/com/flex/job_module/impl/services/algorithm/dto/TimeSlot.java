package com.flex.job_module.impl.services.algorithm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlot {
    private boolean best;
    private Map<Integer, LocalTime> pointTimeSlots;
}
