package com.flex.user_module.impl.services.helpers;

import com.flex.common_module.CommonMethods;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.constants.JobTrackStatus;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.services.helper.JobServiceHelper;
import com.flex.user_module.impl.entities.AgentJob;
import com.flex.user_module.impl.entities.AgentLogin;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.AgentJobRepository;
import com.flex.user_module.impl.repositories.AgentLoginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.loadtime.Agent;
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
    private final JobServiceHelper jobServiceHelper;
    private final AgentLoginRepository agentLoginRepository;

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

            agentJob.setExpectedDuration(getDuration(jobAtPoint.getStartTime(), jobAtPoint.getEndTime()));

        } else {
            agentJob = agentJobRepository.findByAgent_idAndJobAtPoint_id(user.getId(), jobAtPoint.getId());

            agentJob.setStatus(status);
            agentJob.setEndTime(LocalTime.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE)));

            long duration = getDuration(agentJob.getStartTime(), agentJob.getEndTime());

            agentJob.setExpectedDuration(duration);

            int durationAsPercentage = Math.round(((float) duration / agentJob.getExpectedDuration()) * 100);

            if (durationAsPercentage >= 75) {
                agentJob.setDurationMatched(true);
            }
        }
        agentJobRepository.save(agentJob);
    }

    public void markTheAgentLogin(Integer jobId, User agent) {

        AgentLogin agentLogin = agentLoginRepository.getAgentLogin(agent.getId());

        String note = "Agent " + agent.getFName() + " " + agent.getLName() + " logged in at "
                + CommonMethods.timeFormat(agentLogin.getLoginTime()) + " in "
                + agentLogin.getServicePoint().getName() + ".";

        jobServiceHelper.markTheTrack(jobId, JobTrackStatus.AGENT_LOGIN, JobTrackStatus.AGENT_LOGIN_S, note);
    }

    public void markTheTokenInServing(Integer jobId, User agent, String servicePoint) {
        String note = agent.getFName() + " " + agent.getLName() + " started to serving this appointment related services at "
                + CommonMethods.timeFormat(CommonMethods.getCurrentTime()) + " in " + servicePoint + ".";

        jobServiceHelper.markTheTrack(jobId, JobTrackStatus.IN_SERVING_START, JobTrackStatus.IN_SERVING_START_S, note);
    }

    public void markTheTokenCompleted(Integer jobId, User agent, String servicePoint) {
        String note = agent.getFName() + " " + agent.getLName() + " completed the job- "
                + jobId + " related services at " + servicePoint + ".";

        jobServiceHelper.markTheTrack(jobId, JobTrackStatus.JOB_COMPLETED, JobTrackStatus.JOB_COMPLETED_S, note);
    }
}
