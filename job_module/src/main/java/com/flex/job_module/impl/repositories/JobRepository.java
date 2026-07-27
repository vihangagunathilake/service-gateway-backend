package com.flex.job_module.impl.repositories;

import com.flex.job_module.api.http.DTO.CenterJob;
import com.flex.job_module.api.http.DTO.DailyJobCounts;
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

    @Query("""
        SELECT
            sc.name AS name,
            COALESCE(
                SUM(
                    CASE
                        WHEN j.status <> :cancel
                         AND j.status <> :timeout
                         AND j.appointmentDate=:date
                        THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS jobs
        FROM ServiceCenter sc
        LEFT JOIN Job j ON j.serviceCenter.id = sc.id
        WHERE sc.serviceProvider.id = :provider
        GROUP BY sc.id, sc.name
    """)
    List<CenterJob> getCenterJobsByProvider(@Param("provider") Integer provider, @Param("cancel") Integer cancel,
                                            @Param("timeout") Integer timeout, @Param("date") LocalDate appointmentDate);

    @Query(value = """
        SELECT SUM(IF(j.status <> :timeout, 1, 0)) AS totalJobs,
            SUM(IF(j.status = :pending, 1, 0)) AS pending,
            SUM(IF(j.status = :serving, 1, 0)) AS serving,
            SUM(IF(j.status = :completed, 1, 0)) AS completed,
            SUM(IF(j.status = :onGoing, 1, 0)) AS onGoing,
            SUM(IF(j.status = :transfer, 1, 0)) AS transferred,
            SUM(IF(j.status <> :cancel, j.down_payment, 0)) AS totalEarnings
        FROM jobs j
        LEFT JOIN service_centers cs
            ON j.service_center_id = cs.id
        WHERE cs.service_provider_id = :providerId
          AND j.appointment_date = :appointmentDate
        """, nativeQuery = true)
    DailyJobCounts getJobSummary(
            @Param("providerId") Integer providerId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("pending") Integer pending,
            @Param("serving") Integer serving,
            @Param("completed") Integer completed,
            @Param("onGoing") Integer onGoing,
            @Param("cancel") Integer cancel,
            @Param("timeout") Integer timeout,
            @Param("transfer") Integer transfer);

    @Query("SELECT j.id FROM Job j WHERE j.serviceCenter.serviceProvider.id=:provider AND j.status=:status AND j.appointmentDate=current_date ")
    List<Integer> getJobIdsByStatusAndProvider(@Param("status")Integer status, @Param("provider")Integer provider);

    @Query(value = """
        SELECT j.id
        FROM jobs j
        WHERE j.status = 0
          AND j.appointment_date = CURDATE()
          AND j.id IN (
              SELECT job_id
              FROM (
                  SELECT job_id
                  FROM jobs_at_point
                  GROUP BY job_id
                  HAVING MIN(start_time) <= DATE_SUB(CURTIME(), INTERVAL 20 MINUTE)
              ) x
          )
        """, nativeQuery = true)
    List<Integer> findTimeoutJobIds();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE jobs
            SET status = 5
            WHERE id IN (:jobIds)
        """, nativeQuery = true)
    void timeoutJobs(@Param("jobIds") List<Integer> jobIds);
}
