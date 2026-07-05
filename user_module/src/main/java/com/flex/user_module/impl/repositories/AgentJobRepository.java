package com.flex.user_module.impl.repositories;

import com.flex.user_module.api.DTO.AgentJobInfo;
import com.flex.user_module.impl.entities.AgentJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AgentJobRepository extends JpaRepository<AgentJob, Integer> {
    AgentJob findByAgent_idAndJobAtPoint_id(Integer agent_id, Integer job_at_point_id);

    AgentJob findByServicePoint_idAndJobAtPoint_id(Integer servicePointId, Integer jobAtPointId);

    AgentJob findByJobAtPoint_id(Integer jobAtPointId);

    @Query("SELECT a FROM AgentJob a WHERE a.agent.id=:agentId AND a.addedDate=:date AND a.status = 1 " +
            "GROUP BY a.jobAtPoint.job.id")
    List<AgentJob> getAgentServingJobs(@Param("agentId") Integer agentId,
                                         @Param("date") LocalDate date);

    @Query("SELECT a FROM AgentJob a WHERE a.agent.id=:agentId AND a.servicePoint.id=:pointId AND a.addedDate=:date AND a.status = 1 " +
            "GROUP BY a.jobAtPoint.job.id")
    List<AgentJob> getServingJobByAgentInPoint(@Param("agentId")Integer agentId, @Param("pointId")Integer pointId,
                                               @Param("date") LocalDate date);

    @Query("SELECT a FROM AgentJob a WHERE a.agent.id=:agentId " +
            "AND a.servicePoint.id <> :pointId " +
            "AND a.addedDate=:date " +
            "AND a.status = 1 " +
            "GROUP BY a.jobAtPoint.job.id")
    List<AgentJob> getServingJobByAgentAndNotInPoint(@Param("agentId")Integer agentId, @Param("pointId")Integer pointId,
                                               @Param("date") LocalDate date);

    @Query("SELECT a FROM AgentJob a WHERE a.jobAtPoint.job.id=:jobId AND a.servicePoint.id <> :pointId AND a.addedDate=:date AND a.status = 1")
    List<AgentJob> getServingJobsAtOtherPointsRelatedToJob(@Param("jobId") Integer jobId, @Param("pointId") Integer pointId,
                                              @Param("date") LocalDate date);

    @Query("SELECT a FROM AgentJob a WHERE a.agent.id=:agentId AND a.addedDate=:date AND a.status = 2")
    List<AgentJob> getAgentCompletedJobs(@Param("agentId") Integer agentId,
                                         @Param("date") LocalDate date);

    @Query("SELECT a FROM AgentJob a " +
            "WHERE a.agent.id=:agentId AND a.addedDate=:date AND a.status = 2 AND a.expectedEndTime >= a.endTime AND a.durationMatched = true")
    List<AgentJob> getAgentSuccessfullyCompletedJobs(@Param("agentId") Integer agentId,
                                         @Param("date") LocalDate date);

    @Query("""
        SELECT
            aj.addedDate AS addedDate,
            aj.jobAtPoint.job.customer.customer as customer,
            aj.jobAtPoint.job.customer.phone as customerMobile,
            aj.jobAtPoint.job.id as jobId,
            aj.jobAtPoint.service.name as service,
            aj.servicePoint.name as servicePointName,
            FUNCTION('TIME_FORMAT', aj.startTime, '%H:%i:%s') AS startedTime,
            FUNCTION('TIME_FORMAT', aj.endTime, '%H:%i:%s') AS endedTime,
            aj.expectedStartTime AS expectedStartTime,
            aj.expectedEndTime AS expectedEndTime,
            aj.duration AS duration,
            aj.expectedDuration AS expectedDuration,
            ROUND((aj.duration * 100.0 / aj.expectedDuration), 1) AS durationRate
        FROM AgentJob aj
        WHERE aj.addedDate = :date
          AND aj.status = 4 AND aj.jobAtPoint.allowToServe = true
    """)
    List<AgentJobInfo> findAgentJobs(@Param("date") LocalDate date);
}
