package com.flex.job_module.impl.services.algorithm.dto;

import com.flex.service_module.impl.entities.ServicePoint;
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

    //define you can directly create job using this time
    //no need to analyze best time slot
    private boolean best;
    private ServicePoint servicePoint;
    private LocalTime possibleStartTime;
    //gap between last job end time and next possible start time
    //helps analyze the best point and time slot
    //always choose minimum gap
    private long gap;
}
