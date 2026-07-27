package com.flex.job_module.impl.services.algorithm;

import com.flex.common_module.CommonMethods;
import com.flex.job_module.api.http.responses.PreparedJobV2;
import com.flex.job_module.impl.entities.Customer;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.job_module.impl.services.helper.JobServiceHelper;
import com.flex.job_module.impl.services.algorithm.dto.TimeSlot;
import com.flex.service_module.impl.entities.*;
import com.flex.service_module.impl.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static com.flex.common_module.constants.Colors.*;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class PrepareJobSubMethods {

    private final JobRepository jobRepository;
    private final JobAtPointRepository jobAtPointRepository;
    private final AvailableServiceRepository availableServiceRepository;

    private final JobServiceHelper jobServiceHelper;

    public PreparedJobV2 loopServicesAndScheduleJobs(List<Service> services,
                                                     List<ServicePoint> servicePointList,
                                                     Job job,
                                                     Customer customer,
                                                     LocalTime nextStartTime,
                                                     LocalDate appointmentDate) {

        int downPayment = 0;
        int totalPayment = 0;

        List<Service> noJobsServices = new ArrayList<>(services);

        // loop services
        for (com.flex.service_module.impl.entities.Service service : services) {
            System.out.println(" ");
            log.info("service: " + service.getName());
            // loop service points to find the best slot
            log.info("next start 1: {}", nextStartTime);
            nextStartTime = loopPointsAndFindNextJobStartTime(servicePointList, service, job,
                    noJobsServices, appointmentDate, nextStartTime);
            log.info("next start 1.1: {}", nextStartTime);
            if (nextStartTime == null) {
                jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                return null;
            }

            //Remove service from this list if create a new job for it.
            // So we can use this list to find free slots, can use to calculate the total time that free slot should have
            noJobsServices.remove(service);

            downPayment = downPayment + service.getDownPrice();
            totalPayment = totalPayment + service.getTotalPrice();
            System.out.println(" ");
        }

        job.setDownPayment(downPayment);
        job.setTotalPrice(totalPayment);

        Job newJob = jobRepository.save(job);

        List<JobAtPoint> jp = jobAtPointRepository.findAllByJobIdOrderByStartTime(newJob.getId());

        List<PreparedJobV2.NewJobsAtPoint> newJobsAtPointList = new ArrayList<>();

        LocalTime appointmentTime = null;

        for (JobAtPoint j: jp) {
            PreparedJobV2.NewJobsAtPoint newJobsAtPoint = PreparedJobV2.NewJobsAtPoint.builder()
                    .id(j.getId())
                    .service(j.getService().getName())
                    .servicePoint(j.getServicePoint().getName())
                    .startTime(CommonMethods.timeFormat(j.getStartTime()))
                    .endTime(CommonMethods.timeFormat(j.getEndTime()))
                    .customer(job.getCustomer().getName())
                    .build();

            if (appointmentTime == null) {
                appointmentTime = j.getStartTime();
            }

            newJobsAtPointList.add(newJobsAtPoint);
        }

        newJob.setAppointmentTime(appointmentTime);
        jobRepository.save(newJob);

        return PreparedJobV2.builder()
                .jobId(job.getId())
                .customerId(customer.getId())
                .appointmentDate(appointmentDate.toString())
                .appointmentTime(CommonMethods.timeFormat(appointmentTime))
                .jobsAtPoint(newJobsAtPointList).build();

    }

    private LocalTime loopPointsAndFindNextJobStartTime(List<ServicePoint> servicePointList,
                                           Service service,
                                           Job job,
                                           List<Service> noJobsServices,
                                           LocalDate appointmentDate,
                                           LocalTime nextStartTime) {

        //this contains best times of points.
        // this helps
        Map<ServicePoint, LocalTime> pointTime = new HashMap<>();
        //this contains gaps between last job time and best time of points.
        // this helps
        Map<ServicePoint, Long> pointGap = new HashMap<>();

        for (ServicePoint servicePoint : servicePointList) {

            log.info("{}looping : {}{}", YELLOW, servicePoint.getName(), RESET);

            AvailableService availableService = availableServiceRepository
                    .availableServiceV2(service.getId(), servicePoint.getId());

            // if this service is available at this point
            TimeSlot timeSlot = findTheBestSlotForTheService(availableService, servicePoint, service, job,
                    noJobsServices, appointmentDate, nextStartTime);

            if (timeSlot != null) {
                //service available for all points and all points has other jobs as well
                //so find the best point with best time slot
                if (!timeSlot.isBest()) {
                    //to analyze best point and time slot later, add point and time to this map.
                    pointTime.put(timeSlot.getServicePoint(), timeSlot.getPossibleStartTime());
                    pointGap.put(timeSlot.getServicePoint(), timeSlot.getGap());
                } else {
                    //best slot and point already founded, no need to analyze best point and time slot
                    pointTime.clear();
                    pointGap.clear();
                    log.info("{} ✅ - as the best", timeSlot.getServicePoint().getName());
                    log.info("starting at {}", nextStartTime);
                    log.info("ending at {}", timeSlot.getPossibleStartTime());
                    return timeSlot.getPossibleStartTime();
                }
            }

        }

        // best point and slot analyzing
        if (!pointTime.isEmpty()) {

            ServicePoint bestPoint = pointGap.entrySet()
                    .stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (bestPoint == null) {
                return null;
            }

            log.info("{} ✅ - analyzed", bestPoint.getName());

            LocalTime bestPointTime = pointTime.get(bestPoint);

            log.info("starting at {}", bestPointTime);

            JobAtPoint jobAtPoint = jobServiceHelper
                    .createJobAtPoint(bestPoint,
                            service,
                            job, bestPointTime,
                            true);

            if (jobAtPoint == null) {
                return null;
            }

            log.info("ending at {}", jobAtPoint.getEndTime());

            jobAtPointRepository.save(jobAtPoint);

            // set next start time to new job end time
            return jobAtPoint.getEndTime();
        }

        return null;
    }

    private TimeSlot findTheBestSlotForTheService(AvailableService availableService,
                                               ServicePoint servicePoint,
                                               Service service,
                                               Job job,
                                               List<Service> noJobsServices,
                                               LocalDate appointmentDate,
                                               LocalTime nextStartTime) {
        if (availableService != null) {
            List<JobAtPoint> previousJobs = jobAtPointRepository
                    .getCompressedJobsByPoint(servicePoint.getId(), appointmentDate);

            if (!previousJobs.isEmpty()) {
                // check any previously created sub jobs which belongs to main job, are in this point

                //for that get previous job ids and check they contain main job id
                List<Integer> prevJobIds = previousJobs.stream().map(
                        JobAtPoint::getMainJobId
                ).toList();

                if (prevJobIds.contains(job.getId())) {
                    //create the new sub job
                    JobAtPoint jobAtPoint = jobServiceHelper
                            .createJobAtPoint(servicePoint,
                                    availableService.getService(),
                                    job, nextStartTime,
                                    true);

                    if (jobAtPoint == null) {
                        return null;
                    }

                    jobAtPointRepository.save(jobAtPoint);

                    // set next start time to new job end time
                    nextStartTime = jobAtPoint.getEndTime();

                    return TimeSlot.builder()
                            .best(true)
                            .servicePoint(servicePoint)
                            .possibleStartTime(nextStartTime)
                            .build();

                } else {

                    //previous jobs are not related to same job id.so in here we have to find the best time slot.
                    // rely on job start time.

                    //be careful. this can contain services which are not available in this point, make the no job services total time calculation wrong
                    List<Integer> noJobsServicesIds = noJobsServices.stream().map(Service::getId).toList();

                    //check the available services for this point using no job services ids.
                    List<Service> noJobsButAvailableServices = availableServiceRepository
                            .getAvailableServicesIds(servicePoint.getId(), noJobsServicesIds);

                    // calculate total service time for no jobs but available services in this point.
                    long totalServiceTimeForNoJobsServices = noJobsButAvailableServices.stream()
                            .filter(s -> s.getServiceTime() != null)
                            .mapToLong(s -> s.getServiceTime().toSecondOfDay())
                            .sum();

                    LocalTime possibleEndTime = null;

                    // check any gaps between jobs and save gap times.
                    // then compare gap start time with next start time.
                    // if next start time > gap start time and possible end < gap end time.
                    // if true this is the one.
                    // else check the gap start > possible end and next start < gap start(free slot has at the start).
                    // if that is true, then this is the one.
                    LocalTime freeSlotStartTime = freeSlotStartTime(previousJobs, nextStartTime,
                            totalServiceTimeForNoJobsServices, servicePoint.getCloseTime());


                    //if no created jobs for this new job or this point is not empty,
                    // calculate the best time slot for this point. This will hold that time.
                    LocalTime oldNextStartTime = nextStartTime;

                    //if freeSlotStartTime is null it means no free slots in here.
                    if (freeSlotStartTime != null) {
                        // possible end time for all no jobs services.
                        possibleEndTime = jobServiceHelper.calculateEndTime(freeSlotStartTime, service.getServiceTime(),
                                servicePoint.getCloseTime());

                        if (possibleEndTime == null) {
                            return null;
                        }
                        nextStartTime = freeSlotStartTime;
                    }

                    return TimeSlot.builder()
                            .best(false)
                            .servicePoint(servicePoint)
                            .possibleStartTime(nextStartTime)
                            .gap(CommonMethods.getDuration(oldNextStartTime, possibleEndTime))
                            .build();
                }

            } else {

                //empty slot, create a new sub job by using next start time and save it
                JobAtPoint jobAtPoint = jobServiceHelper
                        .createJobAtPoint(servicePoint,
                                availableService.getService(),
                                job, nextStartTime,
                                true);

                if (jobAtPoint == null) {
                    return null;
                }

                jobAtPointRepository.save(jobAtPoint);

                // set next start time to new job end time
                nextStartTime = jobAtPoint.getEndTime();

                return TimeSlot
                        .builder()
                        .best(true)
                        .servicePoint(servicePoint)
                        .possibleStartTime(nextStartTime)
                        .build();
            }
        }

        return null;
    }

    private LocalTime freeSlotStartTime(List<JobAtPoint> previousJobs, LocalTime startTime,
                                        long totalServiceTimeForNoJobsServices, LocalTime servicePointCloseTime) {
        log.info("--- free slot analyzing start ---");
        int i;
        i = 0;

        LocalTime possibleEndTime;
        LocalTime lastStartTime = startTime;

        boolean hasOnlyOneJobButBeforeThatHasFreeSlot = i == previousJobs.size() - 1
                && previousJobs.getFirst().getStartTime().isAfter(lastStartTime);

        if (hasOnlyOneJobButBeforeThatHasFreeSlot) {
            return lastStartTime;
        }

        while (i < previousJobs.size() - 1) {
            JobAtPoint firstJob = previousJobs.get(i);
            JobAtPoint secondJob = previousJobs.get(i + 1);

            // free slot has at the very first time
            boolean hasASlotBeforeFirstJob = (i == 0 && lastStartTime.isBefore(firstJob.getStartTime()));

            if (hasASlotBeforeFirstJob) {
                log.info("has time before all jobs: {}", CommonMethods.timeFormat(startTime));
                return startTime;
            }

            startTime = firstJob.getEndTime();
            possibleEndTime = jobServiceHelper.calculateEndTime(startTime,
                            CommonMethods.secondsToLocalTime(totalServiceTimeForNoJobsServices),
                    servicePointCloseTime);

            boolean intervalMatchBetweenFirstAndSecondJob = !startTime.isBefore(firstJob.getEndTime())
                    && !possibleEndTime.isAfter(secondJob.getStartTime());

            if (intervalMatchBetweenFirstAndSecondJob) {
                log.info("has between two jobs: {}", CommonMethods.timeFormat(lastStartTime));
                return lastStartTime;
            }

            if (lastStartTime.isBefore(secondJob.getEndTime())) {
                lastStartTime = secondJob.getEndTime();
            }

            i++;
        }

        if (i == 0 && previousJobs.size() == 1) {
            log.info("after the only job: {}", previousJobs.getFirst().getEndTime());
            return previousJobs.getFirst().getEndTime();
        }
        log.info("last analyzed time: {}", CommonMethods.timeFormat(lastStartTime));
        log.info("--- free slot analyzing end ---");
        return lastStartTime;
    }

    //check the job creating time(current time) is greater than center open time
    // get start time as current time, else center open time.
    public LocalTime nextStartTime(LocalTime serviceCenterOpenTime, LocalDate appointmentDate) {
        if (CommonMethods.getCurrentDate().equals(appointmentDate)
                && CommonMethods.getCurrentTime().isAfter(serviceCenterOpenTime)) {
            return CommonMethods.getCurrentTime();
        }

        return serviceCenterOpenTime;
    }
}
