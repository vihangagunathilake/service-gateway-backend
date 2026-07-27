package com.flex.job_module.impl.services.shedulers;

import com.flex.job_module.events.JobTimedOutEvent;
import com.flex.job_module.impl.entities.Customer;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.CustomerRepository;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("Duplicates")
public class JobTimeoutScheduler {
    private final JobAtPointRepository jobAtPointRepository;
    private final JobRepository jobRepository;
    private final CustomerRepository customerRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void timeoutJobs() {

        int updatedJobAtPoints = jobAtPointRepository.timeoutJobAtPoints();

        if (updatedJobAtPoints > 0) {

            List<Integer> jobIds = jobRepository.findTimeoutJobIds();

            if (!jobIds.isEmpty()) {
                jobRepository.timeoutJobs(jobIds);

                log.info("Timed out {} JobAtPoint(s) and {} Job(s). Job IDs: {}",
                        updatedJobAtPoints,
                        jobIds.size(),
                        jobIds);

                // publish events here if needed
                jobIds.forEach(id -> eventPublisher.publishEvent(new JobTimedOutEvent(id)));
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredDummyJobs() {
        LocalDate date = LocalDate.now();

        List<Integer> dummyDataIds = jobAtPointRepository
                .getDummyJobIds(date);

        log.info("dummy jobs checking \uD83D\uDD0E");
        if (!dummyDataIds.isEmpty()) {
            log.info("has dummy jobs");
            LocalTime time = LocalTime.now().minusMinutes(4);

            List<JobAtPoint> expiredJobsAtPoints = jobAtPointRepository.findExpiredDummy(date, time);

            List<Job> expiredJobs = expiredJobsAtPoints.stream().map(
                    JobAtPoint::getJob
            ).toList();

            List<Customer> expiredCustomers = expiredJobs.stream().map(
                    Job::getCustomer
            ).toList();

            if (!expiredJobsAtPoints.isEmpty()) {
                log.info("{} jobs at points destroying \uD83D\uDD25", expiredJobsAtPoints.size());
                jobAtPointRepository.deleteAll(expiredJobsAtPoints);
                jobAtPointRepository.flush();
                log.info("jobs at points destroyed ✅");
            }

            if (!expiredJobs.isEmpty()) {
                log.info("{} jobs destroying \uD83D\uDD25", expiredJobs.size());
                jobRepository.deleteAll(expiredJobs);
                jobAtPointRepository.flush();
                log.info("jobs destroyed ✅");
            }

            if (!expiredCustomers.isEmpty()) {
                log.info("{} customers destroying \uD83D\uDD25", expiredCustomers.size());
                customerRepository.deleteAll(expiredCustomers);
                jobAtPointRepository.flush();
                log.info("customers destroyed ✅");
            }
        } else {
            log.info("no dummy jobs");
        }
    }
}
