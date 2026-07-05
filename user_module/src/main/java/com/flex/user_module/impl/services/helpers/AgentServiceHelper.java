package com.flex.user_module.impl.services.helpers;

import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.user_module.impl.entities.AgentJob;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.AgentJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static com.flex.common_module.CommonMethods.getDuration;
import static com.flex.common_module.constants.AppConstants.ASIA_COLOMBO_TIME_ZONE;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class AgentServiceHelper {

    private final AgentJobRepository agentJobRepository;

    public void servingJob(JobAtPoint jobAtPoint, User user, int status) {
        AgentJob agentJob;
        if (status == JobStatus.IN_SERVICE) {
            agentJob = AgentJob.builder()
                    .agent(user)
                    .jobAtPoint(jobAtPoint)
                    .servicePoint(jobAtPoint.getServicePoint())
                    .startTime(LocalTime.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE)))
                    .endTime(null)
                    .addedDate(LocalDate.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE)))
                    .expectedStartTime(jobAtPoint.getStartTime())
                    .expectedEndTime(jobAtPoint.getEndTime())
                    .customerArrivedTime(jobAtPoint.getCustomerArrivedTime())
                    .status(JobStatus.IN_SERVICE)
                    .build();

            Duration duration = getDuration(jobAtPoint.getStartTime(), jobAtPoint.getEndTime());

            agentJob.setExpectedDuration(duration.getSeconds());

        } else {
            agentJob = agentJobRepository.findByAgent_idAndJobAtPoint_id(user.getId(), jobAtPoint.getId());

            agentJob.setStatus(status);
            agentJob.setEndTime(LocalTime.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE)));

            Duration duration = getDuration(agentJob.getStartTime(), agentJob.getEndTime());
            log.info("job start: {}", agentJob.getStartTime());
            log.info("job end: {}", agentJob.getEndTime());
            log.info("re duration: {}", duration.getSeconds());

            agentJob.setExpectedDuration(duration.getSeconds());

            int durationAsPercentage = Math.round(((float) duration.getSeconds() / agentJob.getExpectedDuration()) * 100);

            if (durationAsPercentage >= 75) {
                agentJob.setDurationMatched(true);
            }
        }
        agentJobRepository.save(agentJob);
    }
}
