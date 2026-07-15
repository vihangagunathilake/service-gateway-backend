package com.flex.job_module.impl.services.algorithm;

import com.flex.common_module.CommonMethods;
import com.flex.job_module.api.http.requests.PrepareJob;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.constants.JobTypes;
import com.flex.job_module.impl.entities.Customer;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.CustomerRepository;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.job_module.impl.services.helper.JobServiceHelper;
import com.flex.job_module.impl.services.algorithm.dto.TimeSlot;
import com.flex.service_module.impl.entities.*;
import com.flex.service_module.impl.repositories.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flex.common_module.http.ReturnResponse.BAD_REQUEST;
import static com.flex.common_module.http.ReturnResponse.CONFLICT;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class PrepareJobSubMethods {

    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;
    private final JobAtPointRepository jobAtPointRepository;
    private final AvailableServiceRepository availableServiceRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePointRepository servicePointRepository;
    private final CenterClusterRepository centerClusterRepository;
    private final CCSRepository ccsRepository;

    private final JobServiceHelper jobServiceHelper;

    public ResponseEntity<?> prepareJobV2(PrepareJob prepareJob, HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (prepareJob.getAppointmentDate() == null) {
            return BAD_REQUEST("Appointment date not defined");
        }

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(prepareJob.getServiceCenterId());

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        List<ServicePoint> servicePointList = servicePointRepository.servicePointsByCenter(serviceCenter.getId());

        if (servicePointList.isEmpty()) {
            return CONFLICT("Service points not found");
        }

        Customer customer = customerRepository.findByPhone(prepareJob.getPhone());

        if (customer == null) {
            customer = Customer.builder()
                    .customer(prepareJob.getCustomer())
                    .phone(prepareJob.getPhone())
                    .dummy(true)
                    .build();
        }

        Job hasJobForThisCustomer = jobRepository
                .jobForCustomer(customer.getId());

        if (hasJobForThisCustomer != null) {
            return CONFLICT("Already have job for this customer");
        }

        customerRepository.save(customer);

        Job job = Job.builder()
                .customer(customer)
                .serviceCenter(serviceCenter)
                .appointmentDate(prepareJob.getAppointmentDate())
                .status(JobStatus.PENDING)
                .jobType(JobTypes.WEB)
                .description(prepareJob.getNotes())
                .createdDate(LocalDate.now())
                .createdTime(LocalTime.now())
                .dummy(true)
                .build();

        jobRepository.save(job);

        //this is use after created the jobAtPoint, so this is equals to new jobAtPoint end time.
        // then this going to be a start time for the next jobAtPoint
        LocalTime nextStartTime = nextStartTime(serviceCenter.getOpenTime());

        if (prepareJob.getCenterClusterId() != null) {
            // cluster services
            CenterCluster centerCluster = centerClusterRepository.getCenterClusterById(prepareJob.getCenterClusterId());

            if (centerCluster == null) {
                deleteDummyCustomerAndJob(job, customer);
                return CONFLICT("Cluster not found");
            }

            job.setClusterId(centerCluster.getCluster().getId());

            // find services from center cluster
            List<com.flex.service_module.impl.entities.Service> centerClusterServices = ccsRepository
                    .getServicesByCenterClusterId(prepareJob.getCenterClusterId());

            if (centerClusterServices.isEmpty()) {
                deleteDummyCustomerAndJob(job, customer);
                return CONFLICT("Services not found for cluster");
            }

            //Remove service from this list if create a new job for it.
            // So we can use this list to find free slots, can use to calculate the total time that free slot should have
            List<Service> noJobsServices = centerClusterServices;

            // loop services
            for (com.flex.service_module.impl.entities.Service service : centerClusterServices) {

                // loop service points to find the best slot
                loopServicePointsOfCenter(servicePointList, service, job,
                        noJobsServices, prepareJob, nextStartTime);
            }

        } else {
            //todo custom services
        }

        return null;
    }

    private void loopServicePointsOfCenter(List<ServicePoint> servicePointList,
                                           Service service,
                                           Job job,
                                           List<Service> noJobsServices,
                                           PrepareJob prepareJob,
                                           LocalTime nextStartTime) {
        //this contains best times of points.
        // this helps
        Map<Integer, LocalTime> pointTime = new HashMap<>();

        for (ServicePoint servicePoint : servicePointList) {

            AvailableService availableService = availableServiceRepository
                    .availableService(servicePoint.getId(), servicePoint.getId());

            // if this service is available at this point
            TimeSlot timeSlot = findTheBestSlotForTheService(availableService, servicePoint, service, job,
                    noJobsServices, prepareJob, nextStartTime);

            if (timeSlot != null) {
                if (!timeSlot.isBest()) {
                    pointTime.putAll(timeSlot.getPointTimeSlots());
                } else {
                    pointTime.clear();
                }
            }

        }

        // map analyzing
        if (!pointTime.isEmpty()) {
            //todo: stopped in here
        }
    }

    private TimeSlot findTheBestSlotForTheService(AvailableService availableService,
                                               ServicePoint servicePoint,
                                               Service service,
                                               Job job,
                                               List<Service> noJobsServices,
                                               PrepareJob prepareJob,
                                               LocalTime nextStartTime) {
        if (availableService != null) {
            List<JobAtPoint> previousJobs = jobAtPointRepository
                    .findByServicePointIdAndAppointmentDate(servicePoint.getId(), prepareJob.getAppointmentDate());

            if (!previousJobs.isEmpty()) {
                // check any previously created sub jobs which belongs to main job, are in this point

                //for that get previous job ids and check they contain main job id
                List<Integer> prevJobIds = previousJobs.stream().map(
                        j -> j.getJob().getId()
                ).toList();

                if (prevJobIds.contains(job.getId())) {
                    //create the new sub job
                    JobAtPoint jobAtPoint = jobServiceHelper
                            .createJobAtPoint(servicePoint,
                                    availableService.getService(),
                                    job, nextStartTime,
                                    true);
                    jobAtPointRepository.save(jobAtPoint);

                    // set next start time to new job end time
                    nextStartTime = jobAtPoint.getEndTime();

                    Map<Integer, LocalTime> slots = new HashMap<>();
                    slots.put(servicePoint.getId(), nextStartTime);

                    return TimeSlot.builder().best(true).pointTimeSlots(slots).build();

                } else {
                    //previous jobs are not related to same job id.so in here we have to find the best time slot.
                    // rely on job start time.

                    // calculate the possible end time.
                    long totalServiceTimeForNoJobsServices = noJobsServices.stream()
                            .filter(s -> s.getServiceTime() != null)
                            .mapToLong(s -> s.getServiceTime().toSecondOfDay())
                            .sum();

                    // possible end time for all no jobs services.
                    LocalTime possibleEndTime = jobServiceHelper.calculateEndTime(nextStartTime,
                            CommonMethods.secondsToLocalTime(totalServiceTimeForNoJobsServices),
                            servicePoint.getCloseTime());

                    // check any gaps between jobs and save gap times.
                    // then compare gap start time with next start time.
                    // if next start time > gap start time and possible end < gap end time.
                    // if true this is the one.
                    // else check the gap start > possible end and next start < gap start(free slot has at the start).
                    // if that is true, then this is the one.
                    LocalTime freeSlotStartTime = freeSlotStartTime(previousJobs, nextStartTime, possibleEndTime);

                    //if no created jobs for this new job or this point is not empty,
                    // calculate the best time slot for this point. This will hold that time.
                    LocalTime possibleStartTime;

                    //if freeSlotStartTime is null it means no free slots in here.
                    if (freeSlotStartTime != null) {
                        possibleStartTime = jobServiceHelper.calculateEndTime(freeSlotStartTime, service.getServiceTime(),
                                servicePoint.getCloseTime());
                    } else {
                        // no free slots, get the last time and save it in map with point id.
                        possibleStartTime = jobServiceHelper.calculateEndTime(nextStartTime, service.getServiceTime(),
                                servicePoint.getCloseTime());
                    }

                    Map<Integer, LocalTime> slots = new HashMap<>();
                    slots.put(servicePoint.getId(), possibleStartTime);

                    return TimeSlot.builder().best(false).pointTimeSlots(slots).build();
                }

            } else {
                //empty slot, create a new sub job by using next start time and save it
                JobAtPoint jobAtPoint = jobServiceHelper
                        .createJobAtPoint(servicePoint,
                                availableService.getService(),
                                job, nextStartTime,
                                true);
                jobAtPointRepository.save(jobAtPoint);

                // set next start time to new job end time
                nextStartTime = jobAtPoint.getEndTime();

                Map<Integer, LocalTime> slots = new HashMap<>();
                slots.put(servicePoint.getId(), nextStartTime);

                return TimeSlot.builder().best(true).pointTimeSlots(slots).build();
            }
        }

        return null;
    }

    private LocalTime freeSlotStartTime(List<JobAtPoint> previousJobs, LocalTime startTime, LocalTime endTime) {
        int i;
        i = 0;
        while (i < previousJobs.size() - 1) {

            JobAtPoint firstJob = previousJobs.get(i);
            JobAtPoint secondJob = previousJobs.get(i + 1);

            if (!endTime.isAfter(firstJob.getStartTime())) { //end time =< 1st job startTime
                return startTime;
            } else if (!startTime.isAfter(firstJob.getEndTime())
                    && !endTime.isBefore(secondJob.getStartTime())) { //next start time >= 1st job end time and possible end < second job startTime
                return startTime;
            }
            i++;
        }
        return null;
    }

    //remove dummy customer and company after has a conflict of the process while prepare jobs
    public void deleteDummyCustomerAndJob(Job job, Customer customer) {
        jobRepository.delete(job);
        customerRepository.delete(customer);
    }

    //check the job creating time(current time) is greater than center open time
    // get start time as current time, else center open time.
    public LocalTime nextStartTime(LocalTime serviceCenterOpenTime) {
        if (CommonMethods.getCurrentTime().isAfter(serviceCenterOpenTime)) {
            return CommonMethods.getCurrentTime();
        }

        return serviceCenterOpenTime;
    }
}
