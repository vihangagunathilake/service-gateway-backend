package com.flex.job_module.impl.repositories;

import com.flex.job_module.api.http.DTO.JobDetailsV1;
import com.flex.job_module.impl.entities.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Integer> {
    Job findByIdAndDummyIsTrue(Integer id);

    Job getJobById(Integer id);

    @Query("SELECT j FROM Job j WHERE j.customer.id=:id AND j.status < 2")
    Job jobForCustomer(@Param("id") Integer customerId);

    @Query("SELECT j.id as jobId, j.customer.customer as customer, j.clusterId as service, j.status as status FROM Job j " +
            "WHERE j.serviceCenter.id=:centerId and j.appointmentDate=:appointmentDate")
    List<JobDetailsV1> getJobDetailsLimitedData(@Param("centerId") Integer centerId,
                                                @Param("appointmentDate") LocalDate appointmentDate);

    @Query("SELECT count(j) FROM Job j WHERE j.serviceCenter.serviceProvider.id=:provider AND j.status=:status")
    Integer getJobCountByStatusAndCenter(@Param("status")Integer status, @Param("provider")Integer provider);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE jobs
        SET status = 5
        WHERE status = 0
          AND appointment_date = CURDATE()
          AND id IN (
              SELECT job_id
              FROM (
                  SELECT job_id
                  FROM jobs_at_point
                  GROUP BY job_id
                  HAVING MIN(start_time) <= DATE_SUB(CURTIME(), INTERVAL 20 MINUTE)
              ) x
          )
        """, nativeQuery = true)
    int timeoutJobs();
}
