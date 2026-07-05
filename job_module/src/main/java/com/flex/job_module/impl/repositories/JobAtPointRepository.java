package com.flex.job_module.impl.repositories;

import com.flex.job_module.api.http.DTO.AgentJobs;
import com.flex.job_module.api.http.DTO.JobTimelineProjection;
import com.flex.job_module.api.http.DTO.MinimumServiceTimePoint;
import com.flex.job_module.impl.entities.JobAtPoint;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface JobAtPointRepository extends JpaRepository<JobAtPoint, Integer> {

    List<JobAtPoint> findAllByJobId(Integer jobId);

    @Query("SELECT j.id FROM JobAtPoint j WHERE j.createdDate=:date AND j.dummyEntity = true")
    List<Integer> getDummyJobIds(@Param("date") LocalDate date);

    @Query("SELECT j.id FROM JobAtPoint j WHERE j.servicePoint.id=:servicePointId " +
            "AND j.job.appointmentDate=:appointmentDate AND j.status < 2")
    List<Integer> getPendingJobAtPointIdsByPoint(@Param("servicePointId") Integer servicePointId,
                                                 @Param("appointmentDate") LocalDate appointmentDate);

    @Query("SELECT j.id FROM JobAtPoint j WHERE j.servicePoint.id=:servicePointId " +
            "AND j.job.appointmentDate=:appointmentDate AND j.allowToServe = true AND j.status < 1 GROUP BY j.job.id")
    List<Integer> getOnlyPendingJobAtPointIdsByPoint(@Param("servicePointId") Integer servicePointId,
                                                 @Param("appointmentDate") LocalDate appointmentDate);

    @Query("SELECT j FROM JobAtPoint j WHERE j.servicePoint.id=:servicePointId " +
            "AND j.job.appointmentDate=:appointmentDate AND j.status < 2 ORDER BY j.startTime")
    List<JobAtPoint> getPendingJobsAtPointByPoint(@Param("servicePointId") Integer servicePointId,
                                                 @Param("appointmentDate") LocalDate appointmentDate);

    @Query("SELECT j FROM JobAtPoint j WHERE j.servicePoint.id=:servicePointId AND j.job.id=:jobId " +
            "AND j.job.appointmentDate=:appointmentDate AND j.allowToServe = true AND j.status < 2 ORDER BY j.startTime")
    List<JobAtPoint> getPendingJobsAtPointByPointAndJob(@Param("servicePointId") Integer servicePointId,
                                                        @Param("jobId") Integer jobId,
                                                  @Param("appointmentDate") LocalDate appointmentDate);

    @Query("SELECT j FROM JobAtPoint j " +
            "WHERE j.servicePoint.id=:point " +
            "and j.job.id=:id " +
            "ORDER BY j.startTime ASC")
    List<JobAtPoint> findByServicePointAndJobIdAndAppointmentDate(
            @Param("point") Integer point,
            @Param("id") Integer job,
            @Param("date") LocalDate appointmentDate);

    @Query("""
       SELECT j
       FROM JobAtPoint j
       LEFT JOIN j.job js
       WHERE j.servicePoint.id = :id
         AND js.appointmentDate = :date
       ORDER BY j.startTime ASC
       """)
    List<JobAtPoint> findByServicePointIdAndAppointmentDate(
            @Param("id") Integer servicePointId,
            @Param("date") LocalDate appointmentDate
    );

    List<JobAtPoint> findAllByJobIdAndDummyEntityIsTrue(Integer jobId);

    @Query(value = """
        SELECT sp.id AS servicePointId,
               IFNULL(
                   SEC_TO_TIME(
                       SUM(TIME_TO_SEC(jp.end_time) - TIME_TO_SEC(jp.start_time))
                   ),
                   '00:00:00'
               ) AS totalServiceTime
        FROM service_point sp
        LEFT JOIN jobs_at_point jp ON jp.service_point_id = sp.id
        LEFT JOIN available_services av ON av.service_point_id = sp.id
        WHERE sp.id IN (:points) AND av.service_id IN (:services)
        GROUP BY sp.id
        ORDER BY totalServiceTime LIMIT 1""", nativeQuery = true)
    MinimumServiceTimePoint findServicePointWithMinTotalServiceTime(
            @Param("points") List<Integer> points, @Param("services") List<Integer> services);

    @Query(value = """
        SELECT jp.id AS jobAtPointId,
               j.payment_verified as verified,
            j.id AS jobId,
            c.customer AS customerName,
            s.name AS serviceName,
            CASE WHEN jp.status = 0 THEN 'pending'
                WHEN jp.status = 1 THEN 'serving'
                WHEN jp.status = 2 THEN 'completed'
                WHEN jp.status = 5 THEN 'timeout'
                ELSE 'unknown'
            END AS status,
            jp.start_time AS startTime,
            jp.end_time AS endTime,
            jp.service_point_id as pointId,
            CONCAT(
                DATE_FORMAT(jp.start_time, '%h:%i:%s'),
                ' - ',
                DATE_FORMAT(jp.end_time, '%h:%i:%s')
            ) AS fromTo
        FROM jobs_at_point jp
        LEFT JOIN jobs j ON jp.job_id = j.id
        LEFT JOIN customers c ON c.id = j.customer_id
        LEFT JOIN services s ON s.id = jp.service_id
        WHERE jp.service_point_id = :servicePointId
          AND j.appointment_date = :appointmentDate
        ORDER BY jp.service_point_id, jp.start_time
        """,
            nativeQuery = true)
    List<JobTimelineProjection> getJobTimeline(
            @Param("servicePointId") Integer servicePointId,
            @Param("appointmentDate") LocalDate appointmentDate
    );

    @Query("""
        SELECT j
        FROM JobAtPoint j
        WHERE j.dummyEntity = true
        AND (j.createdDate > :date
             OR (j.createdDate = :date AND j.createdTime < :time))
    """)
    List<JobAtPoint> findExpiredDummy(LocalDate date, LocalTime time);

    @Query("SELECT j.job.id FROM JobAtPoint j WHERE j.servicePoint.id=:servicePointId " +
            "AND j.job.appointmentDate=:appointmentDate AND j.allowToServe = true AND j.status < 2 GROUP BY j.job.id")
    List<Integer> getJobsIdsInPoint(@Param("servicePointId") Integer pointId,
                                    @Param("appointmentDate") LocalDate appointmentDate);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE jobs_at_point jap
        JOIN jobs j ON j.id = jap.job_id
        JOIN (
            SELECT job_id
            FROM jobs_at_point
            GROUP BY job_id
            HAVING MIN(start_time) <= DATE_SUB(CURTIME(), INTERVAL 20 MINUTE)
        ) t ON t.job_id = jap.job_id
        SET jap.status = 5
        WHERE jap.status = 0
          AND j.status = 0
          AND j.appointment_date = CURDATE()
        """, nativeQuery = true)
    int timeoutJobAtPoints();

}
