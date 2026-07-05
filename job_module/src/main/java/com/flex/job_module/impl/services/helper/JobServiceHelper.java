package com.flex.job_module.impl.services.helper;

import com.flex.common_module.constants.Colors;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.impl.entities.Customer;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.CustomerRepository;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.service_module.impl.entities.Service;
import com.flex.service_module.impl.entities.ServicePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.flex.common_module.http.ReturnResponse.CONFLICT;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/12/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class JobServiceHelper {

    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;
    private final JobAtPointRepository jobAtPointRepository;

    public JobAtPoint createJobAtPoint(ServicePoint servicePoint, Service service, Job job, LocalTime startTime, boolean dummy) {
        // calculate end time
        // todo: some times we have to add a rest time for this
        LocalTime calculatedEndTime = calculateEndTime(startTime, service.getServiceTime(), servicePoint.getCloseTime());

        if (calculatedEndTime != null) {
            //create and add job at point
            return JobAtPoint.builder()
                    .servicePoint(servicePoint)
                    .service(service)
                    .job(job)
                    .startTime(startTime) //start time is the end time of last job
                    .endTime(calculatedEndTime)
                    .createdDate(LocalDate.now())
                    .createdTime(LocalTime.now())
                    .status(JobStatus.PENDING)
                    .dummyEntity(dummy)
                    .build();
        } else {
            return null;
        }


    }

    public JobAtPoint createJobAtPoint(ServicePoint servicePoint,
                                       Service service,
                                       Job job,
                                       LocalTime startTime,
                                       ServicePoint minimumEndTimePoint,
                                       LocalTime minimumEndTimeAtPoint,
                                       boolean dummy) {
        // calculate end time
        // todo: some times we have to add a rest time for this
        LocalTime calculatedEndTime = calculateEndTime(startTime, service.getServiceTime(), servicePoint.getCloseTime());

        if (calculatedEndTime != null) {
            //create and add job at point
            return JobAtPoint.builder()
                    .servicePoint(servicePoint)
                    .service(service)
                    .job(job)
                    .startTime(startTime) //start time is the end time of last job
                    .endTime(calculatedEndTime)
                    .createdDate(LocalDate.now())
                    .createdTime(LocalTime.now())
                    .status(JobStatus.PENDING)
                    .dummyEntity(dummy)
                    .build();
        } else {
            // no slot found in this day

            // but there is a point which has minimum end time. try this
            if(minimumEndTimePoint != null) {
                List<JobAtPoint> createdJobsSoFar = jobAtPointRepository
                        .findByServicePointAndJobIdAndAppointmentDate(
                                servicePoint.getId(),
                                job.getId(),
                                job.getAppointmentDate()
                        );

                if (createdJobsSoFar.isEmpty()) return null;

                List<JobAtPoint> rescheduledJobs = new ArrayList<>();

                LocalTime newStartTime = minimumEndTimeAtPoint;

                for (JobAtPoint createdJob : createdJobsSoFar) {
                    if (createdJob.getServicePoint().getId().equals(servicePoint.getId())) {
                        LocalTime newEndTime = calculateEndTime(newStartTime,
                                createdJob.getService().getServiceTime(), createdJob.getServicePoint().getCloseTime());

                        if (newEndTime == null) return null;

                        createdJob.setServicePoint(minimumEndTimePoint);
                        createdJob.setStartTime(newStartTime);
                        createdJob.setEndTime(newEndTime);

                        rescheduledJobs.add(createdJob);

                        newStartTime = newEndTime;
                    }
                }

                jobAtPointRepository.saveAll(rescheduledJobs);

                LocalTime calculatedEndTimeForNext =
                        calculateEndTime(newStartTime, service.getServiceTime(), servicePoint.getCloseTime());

                if (calculatedEndTimeForNext != null) {
                    return JobAtPoint.builder()
                            .servicePoint(minimumEndTimePoint)
                            .service(service)
                            .job(job)
                            .startTime(newStartTime)
                            .endTime(calculatedEndTimeForNext)
                            .createdDate(LocalDate.now())
                            .createdTime(LocalTime.now())
                            .status(JobStatus.PENDING)
                            .dummyEntity(dummy)
                            .build();
                } else {
                    return null;
                }

            } else {
                return null;
            }
        }
    }

    public LocalTime calculateEndTime(LocalTime startTime, LocalTime serviceTime, LocalTime closeTime) {

        LocalTime endTime = startTime
                .plusHours(serviceTime.getHour())
                .plusMinutes(serviceTime.getMinute())
                .plusSeconds(serviceTime.getSecond());

        if (closeTime.isAfter(endTime)) {
            return startTime
                    .plusHours(serviceTime.getHour())
                    .plusMinutes(serviceTime.getMinute())
                    .plusSeconds(serviceTime.getSecond());
        } else {
            return null;
        }
    }

    public void clearDummyData(Integer customerId, Integer jobId) {
        Customer customer = customerRepository.findByIdAndDummyIsTrue(customerId);

        Job job = jobRepository.findByIdAndDummyIsTrue(jobId);

        List<JobAtPoint> jobsAtPoint = jobAtPointRepository.findAllByJobIdAndDummyEntityIsTrue(jobId);

        if (!jobsAtPoint.isEmpty()) {
            jobAtPointRepository.deleteAll(jobsAtPoint);
            jobAtPointRepository.flush();
        }

        if (job != null) {
            jobRepository.delete(job);
            jobRepository.flush();
        }

        if (customer != null) {
            customerRepository.delete(customer);
            customerRepository.flush();
        }
    }

    public LocalTime freeSlotStart(List<JobAtPoint> previousJobs, ServicePoint servicePoint, LocalTime totalServiceTime) {

        LocalTime open = servicePoint.getOpenTime();
        LocalTime close = servicePoint.getCloseTime();

        LocalTime jobStart = open;

        // has jobs
        if (previousJobs != null) {
            for (JobAtPoint job : previousJobs) {
                // this can be null if calculated end time for total service is after the close time
                LocalTime endTimeGoingToBe = calculateEndTime(jobStart, totalServiceTime, close);

                if (endTimeGoingToBe == null) {
                    return null;
                }

                if (endTimeGoingToBe.isBefore(job.getStartTime())) {
                    return jobStart;
                } else {
                    jobStart = job.getEndTime();
                }
            }
        }

        return jobStart;
    }

    public LocalTime findFreeSlot(List<JobAtPoint> previousJobs, ServicePoint servicePoint, LocalTime totalServiceTime) {

        LocalTime open = servicePoint.getOpenTime();
        LocalTime close = servicePoint.getCloseTime();

        LocalTime jobStart = open;

        // has jobs
        if (previousJobs != null) {
            for (JobAtPoint job : previousJobs) {
                if (!jobStart.equals(job.getStartTime())) {
                    // this can be null if calculated end time for total service is after the close time
                    LocalTime endTimeGoingToBe = calculateEndTime(jobStart, totalServiceTime, close);

                    if (endTimeGoingToBe == null) {
                        return null;
                    }

                    if (endTimeGoingToBe.isBefore(job.getStartTime())
                            || endTimeGoingToBe.equals(job.getStartTime())) {
                        return jobStart;
                    }
                }

                jobStart = job.getEndTime();
            }
        }

        // no free slots
        return null;
    }

}
