package com.flex.notification_module.listeners;

import com.flex.common_module.CommonMethods;
import com.flex.job_module.constants.JobTrackStatus;
import com.flex.job_module.events.JobTimedOutEvent;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.job_module.impl.services.helper.JobServiceHelper;
import com.flex.user_module.impl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class TimeoutJobListener {

    private final JobServiceHelper jobServiceHelper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationsToManagement(JobTimedOutEvent jobTimedOutEvent) {

        String note = "This appointment(Job-" + jobTimedOutEvent.jobId()
                + ") has been timed out in "
                + CommonMethods.getCurrentDate()
                + " at " + CommonMethods.getCurrentTime();

        jobServiceHelper.markTheTrack(jobTimedOutEvent.jobId(), JobTrackStatus.TIMEOUT, JobTrackStatus.TIMEOUT_S, note);
    }
}
