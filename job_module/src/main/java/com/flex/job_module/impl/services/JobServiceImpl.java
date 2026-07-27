package com.flex.job_module.impl.services;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.constants.Colors;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.job_module.api.http.DTO.JobDetailsV1;
import com.flex.job_module.api.http.DTO.JobTimelineProjection;
import com.flex.job_module.api.http.DTO.MinimumServiceTimePoint;
import com.flex.job_module.api.http.requests.PointJobs;
import com.flex.job_module.api.http.requests.PrepareJob;
import com.flex.job_module.api.http.requests.TransferJob;
import com.flex.job_module.api.http.responses.*;
import com.flex.job_module.api.services.JobService;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.constants.JobTrackStatus;
import com.flex.job_module.constants.JobTypes;
import com.flex.job_module.events.NoAgentInPointEvent;
import com.flex.job_module.impl.entities.Customer;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.CustomerRepository;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.job_module.impl.services.algorithm.PrepareJobSubMethods;
import com.flex.job_module.impl.services.helper.*;
import com.flex.service_module.impl.entities.*;
import com.flex.service_module.impl.repositories.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.flex.common_module.constants.AppConstants.ASIA_COLOMBO_TIME_ZONE;
import static com.flex.common_module.http.ReturnResponse.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/11/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class JobServiceImpl implements JobService {

    private final ClusterRepository clusterRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final CenterClusterRepository centerClusterRepository;
    private final CCSRepository ccsRepository;
    private final AvailableServiceRepository availableServiceRepository;
    private final CustomerRepository customerRepository;

    private final JobAtPointRepository jobAtPointRepository;
    private final JobRepository jobRepository;
    private final ServicePointRepository servicePointRepository;

    private final JobServiceHelper jobServiceHelper;
    private final ServicesRepository servicesRepository;
    private final PrepareJobSubMethods prepareJobSubMethods;

    private final ApplicationEventPublisher publisher;

    @Transactional
    @Override
    public ResponseEntity<?> prepareJob(PrepareJob prepareJob, HttpServletRequest request) {
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

        // this take as all service points are empty at the start, this is using to address the nextStartTime issue
        // for empty service points.
        // * if the service point has no jobs, the nextStartTime must be equal to the last created jobs end time
        List<Integer> emptyServicePoints =
                new ArrayList<>(
                        servicePointList.stream()
                                .map(ServicePoint::getId)
                                .toList()
                );

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

        if (prepareJob.getCenterClusterId() != null) {

            log.info(" -- cluster through -- ");

            CenterCluster centerCluster = centerClusterRepository.getCenterClusterById(prepareJob.getCenterClusterId());

            if (centerCluster == null) {
                jobRepository.delete(job);
                customerRepository.delete(customer);
                return CONFLICT("Cluster not found");
            }

            job.setClusterId(centerCluster.getCluster().getId());

            // find services from center cluster
            List<com.flex.service_module.impl.entities.Service> centerClusterServices = ccsRepository
                    .getServicesByCenterClusterId(prepareJob.getCenterClusterId());

            if (centerClusterServices.isEmpty()) {
                jobRepository.delete(job);
                customerRepository.delete(customer);
                return CONFLICT("Services not found for cluster");
            }

            Set<JobAtPoint> jobsAtPoint = new TreeSet<>(
                    Comparator.comparing(JobAtPoint::getStartTime)
            );

            // this use to calculate down payment and save in job entity
            int totalDownPayment = 0;

            //this is useful when find the free slots among other jobs.
            long totalServiceTime = centerClusterServices.stream()
                    .filter(s -> s.getServiceTime() != null)
                    .mapToLong(s -> s.getServiceTime().toSecondOfDay())
                    .sum();

//            LocalTime totalServiceTime = LocalTime.ofSecondOfDay(totalSeconds);
            
            // this represents the last created job end time
            // use to define the next job's start time
            LocalTime nextStartTime = serviceCenter.getOpenTime();
            LocalTime lastJobTime = null;

            // this will save all next start times. using for get the appointment time
            List<LocalTime> allStartTimes = new ArrayList<>();

            // loop services and check create the job at point
            for (com.flex.service_module.impl.entities.Service centerClusterService : centerClusterServices) {
                long minimumServiceTimeFromSec = 86400;
                ServicePoint suitablePoint = servicePointList.getFirst();
                LocalTime minimumEndTime = null;

                // algorithm finds slots by minimum service time.
                // But sometimes minimum service times creates end times which is near to the close time.
                // Therefor some free slots may be not visible because of this.
                // To prevent that using below properties.
                LocalTime minimumEndTimeOfPoint = serviceCenter.getCloseTime();
                ServicePoint minimumEndTimePoint = null;

                log.info("{}total service time from sec: {}{}", Colors.YELLOW, LocalTime.ofSecondOfDay(totalServiceTime), Colors.RESET);
                // this is using for avoid create other jobs after breaking the loop. check the usage
                int i = 0;
                log.info(" ");
                log.info("{}service: {}{}", Colors.YELLOW, centerClusterService.getName(), Colors.RESET);
                log.info("{}service time: {}{}", Colors.YELLOW, centerClusterService.getServiceTime(), Colors.RESET);

                for (ServicePoint servicePoint : servicePointList) {
                    log.info("{}point: {}{}", Colors.YELLOW, servicePoint.getName(), Colors.RESET);
                    //must have service in service point
                    AvailableService availableService = availableServiceRepository
                            .availableServiceV1(centerClusterService.getId(), servicePoint.getId());
                    //if have service in service point
                    if (availableService != null) {

                        //check the previous job is related to the current job.
                        List<JobAtPoint> previousJobs = jobAtPointRepository
                                .findByServicePointIdAndAppointmentDate(servicePoint.getId(), prepareJob.getAppointmentDate());

                        if (!previousJobs.isEmpty()) {
                            //get previous job ids
                            List<Integer> prevJobIds = previousJobs.stream().map(
                                    j -> j.getJob().getId()
                            ).toList();

                            if (minimumEndTimeOfPoint.isAfter(previousJobs.getLast().getEndTime())) {
                                minimumEndTimeOfPoint = previousJobs.getLast().getEndTime();
                                minimumEndTimePoint = servicePoint;
                            }

                            if (prevJobIds.contains(job.getId())) {
                                if (!lastJobTime.isAfter(previousJobs.getLast().getEndTime())) {
                                    nextStartTime = lastJobTime;
                                } else {
                                    nextStartTime = previousJobs.getLast().getEndTime();
                                }

                                allStartTimes.add(nextStartTime);
                                JobAtPoint createJobAtPoint = jobServiceHelper
                                        .createJobAtPoint(servicePoint, centerClusterService, job,
                                                nextStartTime, minimumEndTimePoint, minimumEndTimeOfPoint,true);

                                totalDownPayment = totalDownPayment + centerClusterService.getDownPrice();

                                if (createJobAtPoint != null) {
                                    log.info("point: {}", servicePoint.getName() + " ✅");
                                    log.info("job start time: {}" ,nextStartTime);
                                    log.info("job end time: {}", createJobAtPoint.getEndTime());
                                    log.info("Chosen by: has prev jobs in this point");
                                    jobAtPointRepository.save(createJobAtPoint);
                                } else {
                                    jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                                    return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                                }

                                jobsAtPoint.add(createJobAtPoint);
                                nextStartTime = createJobAtPoint.getEndTime();
                                lastJobTime = nextStartTime;
                                log.info("next start time going to be: {}", nextStartTime);
                                break;
                            } else {
                                //find the total service time of all jobs.
                                long totalSeconds = previousJobs.stream()
                                        .mapToLong(j ->
                                                Duration.between(j.getStartTime(), j.getEndTime()).getSeconds()
                                        )
                                        .sum();

                                log.info("👉 totalSeconds: {}", totalSeconds);
                                log.info("👉 minimumServiceTimeFromSec: {}", minimumServiceTimeFromSec);

                                if (totalSeconds < minimumServiceTimeFromSec) {
                                    minimumServiceTimeFromSec = totalSeconds;
                                    suitablePoint = servicePoint;
                                    minimumEndTime = previousJobs.getLast().getEndTime();

                                    boolean hasEmptyPoints = false;

                                    if (!emptyServicePoints.isEmpty()) {
                                        List<Integer> assignPointsForThisService = availableServiceRepository
                                                .pointsByService(centerClusterService.getId());

                                        for (Integer id: assignPointsForThisService) {

                                            List<Integer> jobAtPointIds = jobAtPointRepository
                                                    .getPendingJobAtPointIdsByPoint(id, prepareJob.getAppointmentDate());

                                            if (jobAtPointIds == null || jobAtPointIds.isEmpty()) {
                                                hasEmptyPoints = true;
                                                break;
                                            } else {
                                                emptyServicePoints.remove(id);
                                            }
                                        }
                                    }

                                    log.info("👉 nextStartTime time: {}", nextStartTime);
                                    log.info("👉 minimumEndTime time: {}", minimumEndTime);
                                    long gapSeconds = Duration.between(nextStartTime, minimumEndTime).getSeconds();
                                    log.info("👉 gapSeconds time: {}", gapSeconds);
                                    if (nextStartTime.isBefore(minimumEndTime) && !hasEmptyPoints) {
                                        nextStartTime = minimumEndTime;
                                        allStartTimes.add(nextStartTime);
                                    }
                                    log.info("👉 nextStartTime 2 time: {}", nextStartTime);
                                }
                            } // this point has jobs, but no sub jobs for this creating job
                        } else {

                            allStartTimes.add(nextStartTime);

                            JobAtPoint createJobAtPoint = jobServiceHelper
                                    .createJobAtPoint(servicePoint, centerClusterService, job,
                                            nextStartTime, minimumEndTimePoint, minimumEndTimeOfPoint, true);

                            totalDownPayment = totalDownPayment + centerClusterService.getDownPrice();

                            if (createJobAtPoint != null) {
                                log.info("point: {}", servicePoint.getName() + " ✅");
                                log.info("job start time: {}" ,nextStartTime);
                                log.info("job end time: {}", createJobAtPoint.getEndTime());
                                log.info("Chosen by: no prev jobs in this point");
                                jobAtPointRepository.save(createJobAtPoint);
                            } else {
                                jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                                return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                            }

                            nextStartTime = jobServiceHelper.calculateEndTime(nextStartTime,
                                    centerClusterService.getServiceTime(), servicePoint.getCloseTime());
                            lastJobTime = nextStartTime;
                            allStartTimes.add(nextStartTime);

                            jobAtPointRepository.save(createJobAtPoint);

                            jobsAtPoint.add(createJobAtPoint);
                            log.info("next start time going to be: {}", nextStartTime);
                            break;
                        } // no jobs in this service point
                    } // this service has not available in this point
                    i = i + 1;
                } // go to the next service point

                // if statement is protecting form unnecessary jobs after broke the loop.
                if (servicePointList.size() == i) {

                    LocalTime bestTime;
                    LocalTime freeStart = null;

                    List<JobAtPoint> prevJobs = jobAtPointRepository.getPendingJobsAtPointByPoint(suitablePoint.getId(),
                            prepareJob.getAppointmentDate());

                    // sometimes, there can be some free slots among other jobs.
                    // so find it and assign this job to that slot.
                    if (!prevJobs.isEmpty()) {

                        if (lastJobTime != null) {
                            LocalTime possibleEndTime = jobServiceHelper.calculateEndTime(
                                    lastJobTime, LocalTime.ofSecondOfDay(totalServiceTime), suitablePoint.getCloseTime()
                            );

                            if (possibleEndTime == null) {
                                jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                                return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                            }

                            log.info("👉 possible end time: {}", possibleEndTime);

                            boolean isAvailable = false;

                            for (int x = 0; x < prevJobs.size() - 1; x++) {

                                JobAtPoint first = prevJobs.get(x);
                                JobAtPoint second = prevJobs.get(x + 1);

                                if (first.getEndTime() == null || second.getStartTime() == null) {
                                    continue;
                                }

                                // last job time is equal or grater than 1st job end time
                                // possible end time is equal or less than second job start time
                                boolean fitsInGap =
                                        !lastJobTime.isBefore(first.getEndTime()) &&   // start >= first end
                                                !possibleEndTime.isAfter(second.getStartTime()); // end <= second start

                                // fitsInGap can be false, but if the possible end time is less than 1st job start
                                // but check if the last job time is less than first start time
                                if (!fitsInGap && lastJobTime.isBefore(first.getStartTime())
                                        && !possibleEndTime.isAfter(first.getStartTime())) {
                                    fitsInGap = true;
                                }

                                if (fitsInGap) {
                                    isAvailable = true;
                                    break;
                                }

                                // There is no free slot is available at exact calculated time
                                // But may have free slots after calculated time. Check it

                                long diff = Duration.between(first.getEndTime(), second.getStartTime()).getSeconds();

                                // Yes but last job time must never after second start time
                                // ex: last: 13:15, first end: 9:25 second start 11:00 (wrong version).
                                // Now this false. Should not happen. If it happened the free slot going to be 9:25-11:00
                                // ex: last: can be 9.25-11:00*, first end: 9:25 second start 11:00 (correct version).
                                // *11 can be but different going to be 0. So diff >= totalServiceTime going to be false.
                                boolean isLastJobTimeBefore2ndJobStart
                                        = lastJobTime.isBefore(second.getStartTime());


                                if (diff >= totalServiceTime && isLastJobTimeBefore2ndJobStart) {
                                    //this is that free slot
                                    log.info("free time found \uD83D\uDD0D: {} and {}", first.getEndTime(), second.getStartTime());
                                    freeStart = first.getEndTime();
                                }
                            }

                            // A free slot is available at exact calculated time
                            if (isAvailable) {
                                log.info("has free slot for middle services ✅");
                                long serviceSeconds = centerClusterService.getServiceTime().toSecondOfDay();

                                LocalTime nextStart = lastJobTime; // default start


                                prevJobs.sort(Comparator.comparing(JobAtPoint::getStartTime));

                                for (JobAtPoint prevJob : prevJobs) {

                                    LocalTime jobStart = prevJob.getStartTime();

                                    // ✅ Check gap from nextStart → jobStart
                                    if (nextStart.isBefore(jobStart)) {

                                        long gapSeconds = Duration.between(nextStart, jobStart).getSeconds();

                                        log.info("----- next start time: {}", nextStart);
                                        log.info("----- job start time: {}", jobStart);
                                        log.info("----- gap: {}", gapSeconds);
                                        log.info("----- serviceSeconds: {}", serviceSeconds);

                                        //if this is true, job can fit for this gap.
                                        // ex: possible end time: 10:10, next job start at 10:45. So ✅
                                        //but there can be another slot which can have closest start time
                                        // ex: if this job start: 09:30 end 10:10
                                        if (gapSeconds >= serviceSeconds) {
                                            freeStart = nextStart;
                                            break;
                                        }
                                    }

                                    // Move nextStart forward
                                    LocalTime jobEnd = prevJob.getActualEndTime() != null
                                            ? prevJob.getActualEndTime()
                                            : prevJob.getEndTime();

                                    if (jobEnd.isAfter(nextStart)) {
                                        nextStart = jobEnd;
                                    }
                                }
                            }
                        } else {
                            long serviceSeconds = centerClusterService.getServiceTime().toSecondOfDay();

                            LocalTime nextStart = serviceCenter.getOpenTime(); // default start

                            prevJobs.sort(Comparator.comparing(JobAtPoint::getStartTime));

                            for (JobAtPoint prevJob : prevJobs) {

                                LocalTime jobStart = prevJob.getStartTime();

                                // ✅ Check gap from nextStart → jobStart
                                if (nextStart.isBefore(jobStart)) {

                                    long gapSeconds = Duration.between(nextStart, jobStart).getSeconds();
                                    if (gapSeconds >= serviceSeconds) {
                                        freeStart = nextStart;
                                        break;
                                    }
                                }

                                // Move nextStart forward
                                LocalTime jobEnd = prevJob.getActualEndTime() != null
                                        ? prevJob.getActualEndTime()
                                        : prevJob.getEndTime();

                                if (jobEnd.isAfter(nextStart)) {
                                    nextStart = jobEnd;
                                }
                            }
                        }
                    }
                    if (freeStart != null) {
                        log.info("has free slot for first service ✅");
                        bestTime = freeStart;
                    } else {
                        // 0 means no service points
                        if (minimumServiceTimeFromSec == 0) {
                            jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                            return CONFLICT("No suitable service point for " + centerClusterService.getName());
                        }

                        if (minimumEndTime != null && minimumEndTime.isBefore(nextStartTime)) {
                            bestTime = minimumEndTime;
                            log.info("bestTime: {}", bestTime);
                            log.info("lastJobTime: {}", lastJobTime);

                            if (lastJobTime == null) {
                                lastJobTime = bestTime;
                            }

                            if (bestTime.isBefore(lastJobTime)) {
                                bestTime = lastJobTime;
                            }
                        } else {
                            bestTime = nextStartTime;
                        }
                    }

                    JobAtPoint createJobAtPoint = jobServiceHelper
                            .createJobAtPoint(suitablePoint, centerClusterService, job,
                                    bestTime, minimumEndTimePoint, minimumEndTimeOfPoint, true);

                    totalDownPayment = totalDownPayment + centerClusterService.getDownPrice();

                    if (createJobAtPoint != null) {
                        log.info("point: {}", suitablePoint.getName() + " ✅");
                        log.info("job start time: {}" ,bestTime);
                        log.info("job end time: {}", createJobAtPoint.getEndTime());
                        log.info(freeStart != null ? "Chosen by: has free slot among other jobs"
                                : "Chosen by: minimum service time check");
                        jobAtPointRepository.save(createJobAtPoint);
                    } else {
                        jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                        return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                    }

                    nextStartTime = createJobAtPoint.getEndTime();
                    lastJobTime = nextStartTime;
                    log.info("next start time going to be: {}", nextStartTime);
                    allStartTimes.add(nextStartTime);
                    jobAtPointRepository.save(createJobAtPoint);

                    jobsAtPoint.add(createJobAtPoint);
                }
                totalServiceTime = totalServiceTime - centerClusterService.getServiceTime().toSecondOfDay();
                log.info(" ");
            }

            LocalTime appointmentTime = null;
            if (!allStartTimes.isEmpty()) {
                appointmentTime = allStartTimes.stream().min(LocalTime::compareTo).get();
                log.info("appointmentTime: {}", appointmentTime);
            }

            job.setDownPayment(totalDownPayment);

            job.setAppointmentTime(appointmentTime);
            jobRepository.save(job);

            //cluster completed
            return DATA(PreparedJob.builder()
                    .jobId(job.getId())
                    .customerId(customer.getId())
                    .appointmentDate(prepareJob.getAppointmentDate().toString())
                    .appointmentTime(appointmentTime != null ? appointmentTime.toString() : null)
                    .jobsAtPoint(jobsAtPoint).build());

        } else {

            log.info(" ");
            log.info(" -- custom services -- ");

            if (prepareJob.getServicesIds().isEmpty()) {
                jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                return CONFLICT("Choose services first");
            }

            // custom services
            List<com.flex.service_module.impl.entities.Service> services = servicesRepository
                    .getServicesByIds(prepareJob.getServicesIds());

            // find all services available service points
            List<ServicePoint> points = servicePointRepository
                    .findServicePointsHavingAllServices(prepareJob.getServicesIds(),
                            prepareJob.getServiceCenterId(),
                            prepareJob.getServicesIds().size());

            Set<JobAtPoint> jobsAtPoint = new TreeSet<>(
                    Comparator.comparing(JobAtPoint::getStartTime)
            );

            if (!points.isEmpty()) {

                log.info(" -- all services has some point/points -- ");

                // calculate the total service time for custom services
                long totalSeconds = services.stream()
                        .filter(s -> s.getServiceTime() != null)
                        .mapToLong(s -> s.getServiceTime().toSecondOfDay()) // seconds of the day
                        .sum();

                // convert seconds to Date type
                LocalTime totalServiceTime = LocalTime.ofSecondOfDay(totalSeconds);

                log.info(" ");
                log.info("total service time: {} ", totalServiceTime);

                LocalTime freeSlot = null;
                ServicePoint bestPoint = null;

                for (ServicePoint servicePoint: points) {
                    // get the previous jobs of the point which has minimum service time
                    List<JobAtPoint> previousJobs = jobAtPointRepository
                            .findByServicePointIdAndAppointmentDate(servicePoint.getId(), prepareJob.getAppointmentDate());

                    // find the free slot which is going to be the start time of the first custom service
                    freeSlot = jobServiceHelper.findFreeSlot(previousJobs, servicePoint, totalServiceTime);

                    if (freeSlot != null) {
                        log.info("found free slot among jobs ✅");
                        bestPoint = servicePoint;
                        break;
                    } else {
                        log.info("no free slot among jobs ❌");
                    }
                }

                if (freeSlot == null) {
                    List<Integer> pointIds = servicePointList.stream().map(
                            ServicePoint::getId
                    ).collect(Collectors.toList());

                    // find the service point which has the lowest service time
                    MinimumServiceTimePoint lowestServiceTimePoint = jobAtPointRepository
                            .findServicePointWithMinTotalServiceTime(pointIds, prepareJob.getServicesIds());

                    bestPoint = servicePointRepository.findByIdAndDeletedIsFalse(
                            lowestServiceTimePoint.getServicePointId()
                    );

                    List<JobAtPoint> previousJobsAtSuitablePoint = jobAtPointRepository
                            .getPendingJobsAtPointByPoint(lowestServiceTimePoint.getServicePointId(),
                                    prepareJob.getAppointmentDate());

                    if (previousJobsAtSuitablePoint == null || previousJobsAtSuitablePoint.isEmpty()) {
                        log.info("found free slot in empty point ✅");
                        freeSlot = bestPoint.getOpenTime();
                    } else {
                        log.info("assign job to point which has the lowest service time ✅");
                        for (JobAtPoint jobAtPoint: previousJobsAtSuitablePoint) {
                            log.info("job: {}", jobAtPoint.getService().getName());
                            log.info("start time: {}", jobAtPoint.getStartTime());
                            log.info("end time: {}", jobAtPoint.getEndTime());
                            log.info("last job id: {}", jobAtPoint.getJob().getId());
                            log.info("job id: {}", job.getId());
                        }
                        freeSlot = previousJobsAtSuitablePoint.getLast().getEndTime();
                    }
                }

                log.info("free slot start time: {} ", freeSlot);
                log.info("chosen point: {} ", bestPoint.getName() + " ✅");

                if (freeSlot == null) {
                    jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                    return CONFLICT("No available service slots for " + prepareJob.getAppointmentDate());
                }

                LocalTime nextStartTime = freeSlot;

                //loop services and create job at point dummy list.
                for (com.flex.service_module.impl.entities.Service service: services) {

                    log.info(" ");
                    log.info("service: {}", service.getName());
                    JobAtPoint createJobAtPoint = jobServiceHelper
                            .createJobAtPoint(bestPoint, service, job,
                                    nextStartTime, true);

                    if (createJobAtPoint != null) {
                        log.info("start time: {}", nextStartTime);
                        log.info("end time: {}", createJobAtPoint.getEndTime());
                        jobAtPointRepository.save(createJobAtPoint);
                    } else {
                        jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                        log.info(Colors.YELLOW + "4" + Colors.RESET);
                        return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                    }

                    jobAtPointRepository.save(createJobAtPoint);
                    jobsAtPoint.add(createJobAtPoint);

                    nextStartTime = jobServiceHelper.calculateEndTime(nextStartTime,
                            service.getServiceTime(), bestPoint.getCloseTime());
                    log.info("next start time going to be: {}", nextStartTime);
                    log.info(" ");
                }

                job.setAppointmentTime(freeSlot);
                jobRepository.save(job);

                //custom services completed
                return DATA(PreparedJob.builder()
                        .jobId(job.getId())
                        .customerId(customer.getId())
                        .appointmentDate(prepareJob.getAppointmentDate().toString())
                        .appointmentTime(freeSlot.toString())
                        .jobsAtPoint(jobsAtPoint).build());

            } else {
                log.info(" ");
                log.info(" -- services are in different points -- ");
                LocalTime minimumStartTime = serviceCenter.getCloseTime();
                LocalTime minimumEndTime = null;
                LocalTime nextStartTime = serviceCenter.getOpenTime();
                LocalTime lastJobTime = null;

                long totalServiceTime = services.stream()
                        .filter(s -> s.getServiceTime() != null)
                        .mapToLong(s -> s.getServiceTime().toSecondOfDay())
                        .sum();

                int totalDownPayment = 0;

                for (com.flex.service_module.impl.entities.Service service : services) {
                    log.info(" ");
                    log.info("{}service: {}{}", Colors.YELLOW, service.getName(), Colors.RESET);
                    int i = 0;
                    long minimumServiceTimeFromSec = 86400;
                    ServicePoint suitablePoint = servicePointList.getFirst();
                    LocalTime minimumEndTimeOfPoint = serviceCenter.getCloseTime();
                    ServicePoint minimumEndTimePoint = null;

                    for (ServicePoint servicePoint : servicePointList) {

                        log.info("{}service point: {}{}", Colors.YELLOW, servicePoint.getName(), Colors.RESET);
                        AvailableService availableService = availableServiceRepository
                                .availableServiceV1(service.getId(), servicePoint.getId());

                        //has service in this point?
                        if (availableService != null) {
                            List<JobAtPoint> previousJobs = jobAtPointRepository
                                    .findByServicePointIdAndAppointmentDate(servicePoint.getId(), prepareJob.getAppointmentDate());

                            // has previous jobs in this point
                            if (!previousJobs.isEmpty()) {

                                List<Integer> prevJobIds = previousJobs.stream().map(
                                        j -> j.getJob().getId()
                                ).toList();

                                if (minimumEndTimeOfPoint.isAfter(previousJobs.getLast().getEndTime())) {
                                    minimumEndTimeOfPoint = previousJobs.getLast().getEndTime();
                                    minimumEndTimePoint = servicePoint;
                                }

                                // has contains current job id in previous jobs
                                if (prevJobIds.contains(job.getId())) {
                                    // if has create the dummy entity and break the loop
                                    if (!lastJobTime.isAfter(previousJobs.getLast().getEndTime())) {
                                        nextStartTime = lastJobTime;
                                    } else {
                                        nextStartTime = previousJobs.getLast().getEndTime();
                                    }

                                    JobAtPoint createJobAtPoint = jobServiceHelper
                                            .createJobAtPoint(servicePoint, service, job,
                                                    nextStartTime, minimumEndTimePoint, minimumEndTimeOfPoint, true);

                                    totalDownPayment = totalDownPayment + service.getDownPrice();

                                    if (createJobAtPoint != null) {
                                        log.info("point: {}", servicePoint.getName() + " ✅");
                                        log.info("service time: {}", service.getServiceTime());
                                        log.info("job start time: {}" ,nextStartTime);
                                        log.info("job end time: {}", createJobAtPoint.getEndTime());
                                        log.info("Chosen by: has prev jobs in this point");
                                        jobAtPointRepository.save(createJobAtPoint);
                                    } else {
                                        jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                                        log.info(Colors.YELLOW + "5" + Colors.RESET);
                                        return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                                    }

                                    // keep this. Don't know this is for what
                                    if (nextStartTime.isBefore(previousJobs.getLast().getEndTime())) {
                                        log.info("nextStartTime: {}", nextStartTime);

                                        nextStartTime = previousJobs.getLast().getEndTime();
                                        log.info("end: {}", nextStartTime);
                                    }

                                    jobAtPointRepository.save(createJobAtPoint);
                                    jobsAtPoint.add(createJobAtPoint);
                                    nextStartTime = createJobAtPoint.getEndTime();
                                    lastJobTime = nextStartTime;
                                    log.info("next start time going to be: {}", nextStartTime);
                                    break;
                                } else {
                                    // if not save the total job time along with the min service time point id
                                    //      and jump to next iteration(continue).
                                    long totalSeconds = previousJobs.stream()
                                            .mapToLong(j ->
                                                    Duration.between(j.getStartTime(), j.getEndTime()).getSeconds()
                                            )
                                            .sum();

                                    if (totalSeconds < minimumServiceTimeFromSec) {
                                        minimumServiceTimeFromSec = totalSeconds;
                                        suitablePoint = servicePoint;
                                        minimumEndTime = previousJobs.getLast().getEndTime();

                                        boolean hasEmptyPoints = false;

                                        if (!emptyServicePoints.isEmpty()) {
                                            List<Integer> assignPointsForThisService = availableServiceRepository
                                                    .pointsByService(service.getId());

                                            for (Integer id: assignPointsForThisService) {

                                                List<Integer> jobAtPointIds = jobAtPointRepository
                                                        .getPendingJobAtPointIdsByPoint(id, prepareJob.getAppointmentDate());

                                                if (jobAtPointIds == null || jobAtPointIds.isEmpty()) {
                                                    hasEmptyPoints = true;
                                                    break;
                                                } else {
                                                    emptyServicePoints.remove(id);
                                                }
                                            }
                                        }
                                        if (nextStartTime.isBefore(minimumEndTime)
                                                && !hasEmptyPoints) {
                                            nextStartTime = minimumEndTime;
//                                            allStartTimes.add(nextStartTime);
                                        }
                                    }
                                }

                            } else {
                                //  no previous jobs which means this is the better point, create the new entity and break the loop

                                JobAtPoint createJobAtPoint = jobServiceHelper
                                        .createJobAtPoint(servicePoint, service, job,
                                                nextStartTime, true);

                                if (createJobAtPoint != null) {
                                    log.info("point: {}", servicePoint.getName() + " ✅");
                                    log.info("service time: {}", service.getServiceTime());
                                    log.info("job start time: {}" ,nextStartTime);
                                    log.info("job end time: {}", createJobAtPoint.getEndTime());
                                    log.info("Chosen by: no prev jobs in this point");
                                    jobAtPointRepository.save(createJobAtPoint);
                                } else {
                                    jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                                    return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                                }

                                nextStartTime = jobServiceHelper.calculateEndTime(nextStartTime,
                                        service.getServiceTime(), servicePoint.getCloseTime());

                                lastJobTime = nextStartTime;
                                jobsAtPoint.add(createJobAtPoint);
                                log.info("next start time going to be: {}", nextStartTime);
                                break;
                            }
                        } //to next service point
                        i = i + 1;
                    }

                    if (servicePointList.size() == i) {

                        LocalTime bestTime;
                        LocalTime freeStart = null;

                        List<JobAtPoint> prevJobs = jobAtPointRepository.getPendingJobsAtPointByPoint(suitablePoint.getId(),
                                prepareJob.getAppointmentDate());

                        if (!prevJobs.isEmpty()) {
                            if (lastJobTime != null) {
                                LocalTime possibleEndTime = jobServiceHelper.calculateEndTime(
                                        lastJobTime, LocalTime.ofSecondOfDay(totalServiceTime), suitablePoint.getCloseTime()
                                );

                                if (possibleEndTime == null) {
                                    jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                                    return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                                }

                                boolean isAvailable = false;

                                for (int x = 0; x < prevJobs.size() - 1; x++) {

                                    JobAtPoint first = prevJobs.get(x);
                                    JobAtPoint second = prevJobs.get(x + 1);

                                    if (first.getEndTime() == null || second.getStartTime() == null) {
                                        continue;
                                    }

                                    // last job time is equal or grater than 1st job end time
                                    // possible end time is equal or less than second job start time
                                    boolean fitsInGap =
                                            !lastJobTime.isBefore(first.getEndTime()) &&   // start >= first end
                                                    !possibleEndTime.isAfter(second.getStartTime()); // end <= second start

                                    // fitsInGap can be false, but if the possible end time is less than 1st job start
                                    // but check if the last job time is less than first start time
                                    if (!fitsInGap && lastJobTime.isBefore(first.getStartTime())
                                            && !possibleEndTime.isAfter(first.getStartTime())) {
                                        fitsInGap = true;
                                    }

                                    if (fitsInGap) {
                                        isAvailable = true;
                                        break;
                                    }

                                    // There is no free slot is available at exact calculated time
                                    // But may have free slots after calculated time. Check it
                                    long diff = Duration.between(first.getEndTime(), second.getStartTime()).getSeconds();

                                    // Yes but last job time must never after second start time
                                    // ex: last: 13:15, first end: 9:25 second start 11:00 (wrong version).
                                    // Now this false. Should not happen. If it happened the free slot going to be 9:25-11:00
                                    // ex: last: can be 9.25-11:00*, first end: 9:25 second start 11:00 (correct version).
                                    // *11 can be but different going to be 0. So diff >= totalServiceTime going to be false.
                                    boolean isLastJobTimeBefore2ndJobStart
                                            = lastJobTime.isBefore(second.getStartTime());

                                    if (diff >= totalServiceTime && isLastJobTimeBefore2ndJobStart) {
                                        //this is that free slot
                                        log.info("free time found \uD83D\uDD0D: {} and {}", first.getEndTime(), second.getStartTime());
                                        freeStart = first.getEndTime();
                                    }
                                }

                                // A free slot is available at exact calculated time
                                if (isAvailable) {
                                    log.info("has free slot for middle services ✅");
                                    long serviceSeconds = service.getServiceTime().toSecondOfDay();

                                    LocalTime nextStart = lastJobTime; // default start

                                    prevJobs.sort(Comparator.comparing(JobAtPoint::getStartTime));

                                    for (JobAtPoint prevJob : prevJobs) {

                                        LocalTime jobStart = prevJob.getStartTime();

                                        // ✅ Check gap from nextStart → jobStart
                                        if (nextStart.isBefore(jobStart)) {

                                            long gapSeconds = Duration.between(nextStart, jobStart).getSeconds();
                                            if (gapSeconds >= serviceSeconds) {
                                                freeStart = nextStart;
                                                break;
                                            }
                                        }

                                        // Move nextStart forward
                                        LocalTime jobEnd = prevJob.getActualEndTime() != null
                                                ? prevJob.getActualEndTime()
                                                : prevJob.getEndTime();

                                        if (jobEnd.isAfter(nextStart)) {
                                            nextStart = jobEnd;
                                        }
                                    }
                                }

                            } else {
                                long serviceSeconds = service.getServiceTime().toSecondOfDay();

                                LocalTime nextStart = serviceCenter.getOpenTime(); // default start

                                prevJobs.sort(Comparator.comparing(JobAtPoint::getStartTime));

                                for (JobAtPoint prevJob : prevJobs) {

                                    LocalTime jobStart = prevJob.getStartTime();

                                    // ✅ Check gap from nextStart → jobStart
                                    if (nextStart.isBefore(jobStart)) {

                                        long gapSeconds = Duration.between(nextStart, jobStart).getSeconds();
                                        if (gapSeconds >= serviceSeconds) {
                                            freeStart = nextStart;
                                            break;
                                        }
                                    }

                                    // Move nextStart forward
                                    LocalTime jobEnd = prevJob.getActualEndTime() != null
                                            ? prevJob.getActualEndTime()
                                            : prevJob.getEndTime();

                                    if (jobEnd.isAfter(nextStart)) {
                                        nextStart = jobEnd;
                                    }
                                }
                            }
                        }

                        if (freeStart != null) {
                            bestTime = freeStart;
                        } else {
                            if (minimumEndTime != null && minimumEndTime.isBefore(nextStartTime)) {
                                bestTime = minimumEndTime;
                                if (bestTime.isBefore(lastJobTime)) {
                                    bestTime = lastJobTime;
                                }
                            } else {
                                bestTime = nextStartTime;
                            }
                        }

                        JobAtPoint createJobAtPoint = jobServiceHelper
                                .createJobAtPoint(suitablePoint, service, job,
                                        bestTime, minimumEndTimePoint, minimumEndTimeOfPoint, true);

                        totalDownPayment = totalDownPayment + service.getDownPrice();

                        if (createJobAtPoint != null) {
                            log.info("point: {}", suitablePoint.getName() + " ✅");
                            log.info("service time: {}", service.getServiceTime());
                            log.info("job start time: {}" ,bestTime);
                            log.info("job end time: {}", createJobAtPoint.getEndTime());
                            log.info("Chosen by: minimum service time check");
                            jobAtPointRepository.save(createJobAtPoint);
                        } else {
                            jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                            return CONFLICT("Sorry, No available service slots for " + prepareJob.getAppointmentDate());
                        }

                        nextStartTime = createJobAtPoint.getEndTime();
                        lastJobTime = nextStartTime;
                        if (nextStartTime.isBefore(minimumStartTime)) {
                            minimumStartTime = nextStartTime;
                        }

                        jobAtPointRepository.save(createJobAtPoint);
                        jobsAtPoint.add(createJobAtPoint);
                        log.info("next start time going to be: {}", nextStartTime);
                    }
                    log.info(" ");
                }

                job.setAppointmentTime(minimumStartTime);
                job.setDownPayment(totalDownPayment);
                jobRepository.save(job);

                //custom services completed
                return DATA(PreparedJob.builder()
                        .jobId(job.getId())
                        .customerId(customer.getId())
                        .appointmentDate(prepareJob.getAppointmentDate().toString())
                        .appointmentTime(minimumStartTime != null ? minimumStartTime.toString() : null)
                        .jobsAtPoint(jobsAtPoint).build());
            }
        }
    }

    @Override
    public ResponseEntity<?> prepareJobV2(PrepareJob prepareJob, HttpServletRequest request) {
        // * support current time job creation
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
        LocalTime nextStartTime = prepareJobSubMethods.nextStartTime(serviceCenter.getOpenTime(), prepareJob.getAppointmentDate());

        if (prepareJob.getCenterClusterId() != null) {
            // cluster services
            CenterCluster centerCluster = centerClusterRepository.getCenterClusterById(prepareJob.getCenterClusterId());

            if (centerCluster == null) {
                jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                return CONFLICT("Cluster not found");
            }

            job.setClusterId(centerCluster.getCluster().getId());

            // find services from center cluster
            List<com.flex.service_module.impl.entities.Service> centerClusterServices = ccsRepository
                    .getServicesByCenterClusterId(prepareJob.getCenterClusterId());

            if (centerClusterServices.isEmpty()) {
                jobServiceHelper.clearDummyData(customer.getId(), job.getId());
                return CONFLICT("Services not found for cluster");
            }

            PreparedJobV2 preparedJobV2 = prepareJobSubMethods
                    .loopServicesAndScheduleJobs(centerClusterServices, servicePointList, job,
                    customer, nextStartTime, prepareJob.getAppointmentDate());


            if (preparedJobV2 == null) {
                return CONFLICT("No available slots for this service center for " +  prepareJob.getAppointmentDate());
            }

            String note = "This appointment made by "
                    + customer.getCustomer()
                    + " to "
                    + job.getAppointmentDate()
                    + " in "
                    + job.getServiceCenter().getName();

            jobServiceHelper.markTheTrack(job.getId(), JobTrackStatus.PREPARED, JobTrackStatus.PREPARED_S, note);

            return DATA(preparedJobV2);

        } else {

            // custom services
            List<com.flex.service_module.impl.entities.Service> services = servicesRepository
                    .getServicesByIds(prepareJob.getServicesIds());

            // check points which all customer services are available
            List<ServicePoint> allServicesAvailablePoints = servicePointRepository
                    .findServicePointsHavingAllServices(prepareJob.getServicesIds(),
                            prepareJob.getServiceCenterId(),
                            prepareJob.getServicesIds().size());

            boolean hasAllAvailablePoints = !allServicesAvailablePoints.isEmpty();

            PreparedJobV2 preparedJobV2 = prepareJobSubMethods.loopServicesAndScheduleJobs(services,
                    hasAllAvailablePoints ? allServicesAvailablePoints : servicePointList,
                    job,
                    customer, nextStartTime, prepareJob.getAppointmentDate());

            if (preparedJobV2 == null) {
                return CONFLICT("No available slots for this service center for " +  prepareJob.getAppointmentDate());
            }

            return DATA(preparedJobV2);

        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> jobVerification(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        Job job = jobRepository.getJobById(id);

        if (job == null) {
            return CONFLICT("Job not found");
        }

        boolean thisIsTransferredJob = job.getTransferedJob() != null;

        if (thisIsTransferredJob) {
            Job transferedJob = job.getTransferedJob();

            transferedJob.setStatus(JobStatus.TRANSFER);

            jobRepository.save(transferedJob);

            List<JobAtPoint> transferredJobsAtPoint = jobAtPointRepository.findAllByJobId(transferedJob.getId());

            List<JobAtPoint> updatedTransferredJobs = transferredJobsAtPoint.stream()
                    .peek(e -> e.setStatus(JobStatus.TRANSFER))
                    .toList();

            jobAtPointRepository.saveAll(updatedTransferredJobs);

            String note_1 = "Appointment has been transferred to "
                    + job.getServiceCenter().getName()
                    + ". The new appointment date and new appointment number is "
                    + job.getAppointmentDate() + " and Job - " + job.getId() ;

            jobServiceHelper.markTheTrack(job.getTransferedJob().getId(), JobTrackStatus.TRANSFERRED,
                    JobTrackStatus.TRANSFERRED_S, note_1);

            String note_2 = "This appointment is transferred appointment which is related to Job - "
                    + job.getTransferedJob().getId()
                    + ". The appointment date and time was "
                    + job.getTransferedJob().getAppointmentDate()
                    + " at "
                    + CommonMethods.timeFormat(job.getTransferedJob().getAppointmentTime())
                    + " in "
                    + job.getTransferedJob().getServiceCenter().getName();

            jobServiceHelper.markTheTrack(job.getId(), JobTrackStatus.TRANSFERRED,
                    JobTrackStatus.TRANSFERRED_S, note_2);
        }

        Customer customer = customerRepository.getCustomerById(job.getCustomer().getId());

        if (customer == null) {
            return CONFLICT("Customer not found");
        }

        List<JobAtPoint> jobsAtPoints = jobAtPointRepository.findAllByJobId(id);

        if (!jobsAtPoints.isEmpty()) {
            List<JobAtPoint> updatedJobsAtPoints = jobsAtPoints.stream()
                    .peek(j -> {
                        j.setDummyEntity(false);
                        if(thisIsTransferredJob) {
                            j.setServiceDownPrice(0);
                            j.setServiceTotalPrice(0);
                        }
                    }).toList();

            jobAtPointRepository.saveAll(updatedJobsAtPoints);
        }

        customer.setDummy(false);
        job.setDummy(false);
        job.setPaymentVerified(true);

        customerRepository.save(customer);
        customerRepository.flush();

        jobRepository.save(job);
        jobRepository.flush();

        if (!thisIsTransferredJob) {
            List<com.flex.service_module.impl.entities.Service> services = jobsAtPoints.stream()
                    .map(JobAtPoint::getService).toList();

            int totalPayment = services.stream()
                    .mapToInt(com.flex.service_module.impl.entities.Service::getTotalPrice)
                    .sum();

            String note = "Appointment confirm. Paid the total payment "
                    + totalPayment
                    + "/= at "
                    + job.getCreatedDate()
                    + " in "
                    + job.getServiceCenter().getName();

            jobServiceHelper.markTheTrack(job.getId(), JobTrackStatus.PAYMENT_VERIFIED, JobTrackStatus.PAYMENT_VERIFIED_S, note);
        }

        return SUCCESS("Job Created");
    }

    @Override
    public ResponseEntity<?> removeDummyJob(Integer jobId, Integer customerId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        Customer customer = customerRepository.getCustomerById(customerId);

        if (customer == null) {
            return CONFLICT("Customer not found");
        }

        Job job = jobRepository.findByIdAndDummyIsTrue(jobId);

        if (job == null) {
            return CONFLICT("Job not found");
        }

        List<JobAtPoint> jobsAtPoint = jobAtPointRepository.findAllByJobIdAndDummyEntityIsTrue(jobId);

        if (!jobsAtPoint.isEmpty()) {
            jobAtPointRepository.deleteAll(jobsAtPoint);
            jobAtPointRepository.flush();
        }

        jobRepository.delete(job);
        jobRepository.flush();

        if (customer.isDummy()) {
            customerRepository.delete(customer);
            customerRepository.flush();
        }


        return SUCCESS("");
    }

    @Override
    public ResponseEntity<?> pointWiseJobs(PointJobs pointJobs, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(pointJobs.getServiceCenter());

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        List<ServicePoint> servicePoints = servicePointRepository.servicePointsByCenter(pointJobs.getServiceCenter());

        if (servicePoints.isEmpty()) {
            return DATA(null);
        }

        List<Integer> spIds = servicePoints.stream().map(ServicePoint::getId).toList();
        LocalTime minimumServiceTime = availableServiceRepository.findMinimumServiceTimeByServicePointIds(spIds);

        List<JobsSchedule> jobsSchedules = new ArrayList<>();
        int slotId = 1;

        for (ServicePoint servicePoint : servicePoints) {

            long pointDurationSec = Duration
                    .between(servicePoint.getOpenTime(), servicePoint.getCloseTime())
                    .getSeconds();

            List<JobTimelineProjection> timeline = jobAtPointRepository
                    .getJobTimeline(servicePoint.getId(), pointJobs.getDate());

            if (timeline.isEmpty()) {
                // Entire day is free — emit one full free-slot entry
                jobsSchedules.add(jobServiceHelper.buildFullDayFreeSlot(slotId++, servicePoint));
                continue;
            }

            LocalTime lastEndTime = servicePoint.getOpenTime();
            LocalTime sameJobStartTime = null;
            Integer prevJobId = null;

            for (int i = 0; i < timeline.size(); i++) {
                JobTimelineProjection entry = timeline.get(i);

                // Gap before this job? Emit a free-slot entry
                if (lastEndTime.isBefore(entry.getStartTime())) {
                    long gapSec = Duration.between(lastEndTime, entry.getStartTime()).getSeconds();
                    jobsSchedules.add(jobServiceHelper.buildFreeSlotEntry(
                            slotId++, servicePoint.getName(), lastEndTime, entry.getStartTime(),
                            gapSec, pointDurationSec, minimumServiceTime));
                }

                // Same job as previous? Merge into the existing schedule entry
                if (prevJobId != null && prevJobId.equals(entry.getJobId())) {
                    JobsSchedule existing = jobsSchedules.getLast();
                    long extraSec = Duration.between(entry.getStartTime(), entry.getEndTime()).getSeconds();
                    int extraPercent = jobServiceHelper.toPercent(extraSec, pointDurationSec);
                    existing.setTotalTime(existing.getTotalTime() + extraPercent);
                    existing.setFromTo(
                            CommonMethods.timeFormat(sameJobStartTime) + " - "
                                    + CommonMethods.timeFormat(entry.getEndTime()));
                } else {
                    // New job — track its start time and emit a new job-slot entry
                    if (prevJobId == null) {
                        prevJobId = entry.getJobId();
                    }
                    sameJobStartTime = entry.getStartTime();
                    jobsSchedules.add(jobServiceHelper.buildJobSlotEntry(slotId++, servicePoint.getName(), entry, pointDurationSec));
                }

                lastEndTime = entry.getEndTime();

                // Last item in timeline? Append trailing free slot if room remains
                if (i == timeline.size() - 1) {
                    long trailingSec = Duration.between(lastEndTime, servicePoint.getCloseTime()).getSeconds();
                    if (trailingSec > 0) {
                        jobsSchedules.add(jobServiceHelper.buildFreeSlotEntry(
                                slotId++, servicePoint.getName(), lastEndTime, servicePoint.getCloseTime(),
                                trailingSec, pointDurationSec, minimumServiceTime));
                    }
                }
            }
        }

        return DATA(jobsSchedules);
    }

    @Override
    public ResponseEntity<?> dateWiseJobs(PointJobs pointJobs, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(pointJobs.getServiceCenter());

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        List<JobListDetails> jobListDetailsList = new ArrayList<>();

        List<JobDetailsV1> jobDetails = jobRepository
                .getJobDetailsLimitedData(pointJobs.getServiceCenter(), pointJobs.getDate());

        for (JobDetailsV1 jobDetail : jobDetails) {
            JobListDetails jobListDetails = new JobListDetails();
            // set job id
            jobListDetails.setJobId(jobDetail.getJobId());

            // find services workflow or custom
            if (jobDetail.getService() != null) {
                //cluster
                Cluster cluster = clusterRepository.findByIdAndDeletedIsFalse(jobDetail.getService());

                jobListDetails.setService(cluster.getName());

            } else {
                jobListDetails.setService("Custom");
            }

            List<JobAtPoint> jobAtPoints = jobAtPointRepository.findAllByJobId(jobDetail.getJobId());

            Integer status;

            if (jobAtPoints.stream().anyMatch(j -> j.getStatus() == JobStatus.IN_SERVICE)) {
                status = JobStatus.IN_SERVICE;
            } else if (jobAtPoints.stream().anyMatch(j -> j.getStatus() == JobStatus.TRANSFER)) {
                status = JobStatus.TRANSFER;
            } else {
                status = null;
            }

            int completedPercentage = 0;

            if (status == null) {
                List<JobAtPoint> completedJobs = jobAtPoints.stream()
                        .filter(jobAtPoint -> jobAtPoint.getStatus() == JobStatus.COMPLETED).toList();

                completedPercentage = Math.round(((float) completedJobs.size() / jobAtPoints.size()) * 100);

                if (completedPercentage == 0) {
                    if (jobAtPoints.getFirst().getStatus() == JobStatus.TIMEOUT) {
                        status = JobStatus.TIMEOUT;
                    } else {
                        status = JobStatus.PENDING;
                    }
                } else if (completedPercentage == 100) {
                    status = JobStatus.COMPLETED;
                } else {
                    status = JobStatus.IGNORE;
                }
            }

            // find service point
            List<String> servicePoints = jobAtPoints.stream().map(j -> j.getServicePoint().getName()).distinct().toList();
            jobListDetails.setPoints(servicePoints);

            List<String> services = jobAtPoints.stream().map(j -> j.getService().getName()).distinct().toList();
            jobListDetails.setServices(services);

            // find service slot
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

            LocalTime startTime = jobAtPoints.getFirst().getStartTime();
            LocalTime endTime = jobAtPoints.getLast().getEndTime();

            jobListDetails.setTimeSlot(
                    startTime.format(formatter) + " - " + endTime.format(formatter)
            );

            // find status
            jobListDetails.setStatus(status);
            jobListDetails.setCompletedPercentage(completedPercentage);
            jobListDetails.setAllowToServe(jobAtPoints.getFirst().isAllowToServe());

            jobListDetailsList.add(jobListDetails);
        }

        return DATA(jobListDetailsList);
    }

    @Override
    @Transactional
    public ResponseEntity<?> allowToServe(Integer jobId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        Job job = jobRepository.getJobById(jobId);

        if (job == null) {
            return CONFLICT("Job not found");
        }

        List<JobAtPoint> jobsAtPoints = jobAtPointRepository.findAllByJobId(jobId);

        //get the first job(going to be next). Check it's point has an agent now.
        //if not send notifications to admins and managers.
        Integer loginAgentId = servicePointRepository
                .loginAgentAtPoint(jobsAtPoints.getFirst().getServicePoint().getId());

        log.info("loginAgentId: {}", loginAgentId);

        //if not agent in point, create an event to notify this for notification module
        //it will send notifications for management.
        if (loginAgentId == null) {
            publisher.publishEvent(
                    new NoAgentInPointEvent(
                            jobsAtPoints.getFirst().getServicePoint().getId()
                    )
            );
        }

        for (JobAtPoint jobAtPoint : jobsAtPoints) {
            jobAtPoint.setCustomerArrivedTime(LocalTime.now(ZoneId.of(ASIA_COLOMBO_TIME_ZONE)));
            jobAtPoint.setAllowToServe(true);

            jobAtPointRepository.save(jobAtPoint);
        }

        String note = "Customer has arrived at "
                + CommonMethods.getCurrentTime()
                + " to "
                + job.getServiceCenter().getName() + ".";

        if (loginAgentId == null) {
            note = note + " But no agent were already logged in to the " + jobsAtPoints.getFirst().getServicePoint().getName();
        }

        jobServiceHelper.markTheTrack(job.getId(), JobTrackStatus.CUSTOMER_ARRIVED, JobTrackStatus.CUSTOMER_ARRIVED_S, note);

        return SUCCESS("Customer arrived for " + job.getId());
    }

    @Override
    public ResponseEntity<?> transferJob(TransferJob transferJob, HttpServletRequest request) {
        log.info(request.getRequestURI());

        Job job = jobRepository.getJobById(transferJob.getJobId());

        if (job == null) {
            return CONFLICT("Job not found");
        }

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(transferJob.getCenterId());

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        List<ServicePoint> servicePoints = servicePointRepository
                .findAllByServiceCenter_IdAndDeletedIsFalse(transferJob.getCenterId());

        List<JobAtPoint> jobAtPoints = jobAtPointRepository.findAllByJobId(transferJob.getJobId());

        List<com.flex.service_module.impl.entities.Service> services = jobAtPoints.stream()
                .map(JobAtPoint::getService).toList();

        LocalTime nextStartTime = prepareJobSubMethods.nextStartTime(serviceCenter.getOpenTime(),
                transferJob.getNextAppointmentDate());

        // create new dummy job
        Job newDummyJob = Job.builder()
                .customer(job.getCustomer())
                .serviceCenter(serviceCenter)
                .transferedJob(job)
                .appointmentDate(transferJob.getNextAppointmentDate())
                .status(JobStatus.PENDING)
                .jobType(JobTypes.WEB)
                .description(job.getDescription())
                .createdDate(LocalDate.now())
                .createdTime(LocalTime.now())
                .dummy(true)
                .build();

        Job newJob = jobRepository.save(newDummyJob);

        PreparedJobV2 preparedJobV2 = prepareJobSubMethods
                .loopServicesAndScheduleJobs(services, servicePoints, newJob,
                        job.getCustomer(), nextStartTime, transferJob.getNextAppointmentDate());

        if (preparedJobV2 == null) {
            return CONFLICT("No available slots for this service center for " +  transferJob.getNextAppointmentDate());
        }

        return DATA(preparedJobV2);
    }
}
