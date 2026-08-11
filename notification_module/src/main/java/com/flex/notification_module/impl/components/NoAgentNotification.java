package com.flex.notification_module.impl.components;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.constants.NotificationConstants;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.entities.UserNotification;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.notification_module.kafka.topics.KafkaNotificationTopics;
import com.flex.service_module.impl.entities.ServicePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.flex.common_module.http.ReturnResponse.CONFLICT;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class NoAgentNotification {

    private final NotificationTypeRepository notificationTypeRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final JobAtPointRepository jobAtPointRepository;

    private final NotificationComponent notificationComponent;

    public void solveNoAgentNotification(Job job) {

        NotificationType notificationType = notificationTypeRepository
                .getNotificationTypeByType(NotificationConstants.NO_AGENT_FOR_JOB);

        if (notificationType == null) {
            log.warn("solveNoAgentNotification - notificationType is null ");
            return;
        }

        List<UserNotification> userNotifications = userNotificationRepository
                .getUserNotificationsByJobId(job.getId(), notificationType.getId());

        for (UserNotification userNotification : userNotifications) {
            userNotification.setAlreadySolved(true);
            userNotification.setSolvedDate(CommonMethods.getCurrentDate());
            userNotification.setSolvedTime(CommonMethods.getCurrentTime()
            );

            userNotificationRepository.save(userNotification);
        }

//        List<JobAtPoint> jobAtPoints = jobAtPointRepository.findAllByJobId(job.getId());
//
//        ServicePoint servicePoint;
//
//        servicePoint = jobAtPoints.stream()
//                .filter(jobAtPoint -> jobAtPoint.getStatus() == JobStatus.PENDING)
//                .map(JobAtPoint::getServicePoint)
//                .findFirst()
//                .orElse(null);
//
//        if (servicePoint == null) {
//            log.warn("solveNoAgentNotification - servicePoint is null ");
//            return;
//        }
//
//        notificationComponent.send(servicePoint, job,
//                NotificationConstants.NO_AGENT_FOR_JOB,
//                KafkaNotificationTopics.NO_AGENT_IN_POINT_TOPIC);
    }
}
