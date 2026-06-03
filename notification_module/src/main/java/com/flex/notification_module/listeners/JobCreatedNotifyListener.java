package com.flex.notification_module.listeners;

import com.flex.common_module.constants.NotificationConstants;
import com.flex.common_module.http.requests.NotificationMessage;
import com.flex.job_module.events.JobCreatedNotifyEvent;
import com.flex.notification_module.impl.entities.Notification;
import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.entities.UserNotification;
import com.flex.notification_module.impl.repositories.NotificationAccessRepository;
import com.flex.notification_module.impl.repositories.NotificationRepository;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.notification_module.impl.services.helpers.NotificationServiceHelper;
import com.flex.user_module.impl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class JobCreatedNotifyListener {

    private final SimpMessagingTemplate messagingTemplate;

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationAccessRepository notificationAccessRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    private final NotificationServiceHelper notificationServiceHelper;

    @Value("${app.frontend.url}")
    private String baseUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRoleCreated(JobCreatedNotifyEvent jobCreatedNotifyEvent) {
        log.info("role base notifications creation started");

        String description = "New job created in " + jobCreatedNotifyEvent.serviceCenter();

        Notification notification = createNotification(jobCreatedNotifyEvent, description);

        NotificationType notificationType =
                notificationTypeRepository.getNotificationTypeByType(
                        NotificationConstants.JOB_CREATED
                );

        if (notificationType == null) {
            log.warn("Notification type not found");
            return;
        }

        List<User> users =
                notificationAccessRepository.getUsersByTypeAndServiceProvider(
                        notificationType.getId(),
                        jobCreatedNotifyEvent.serviceCenterId()
                );

        log.info("Users count: {}", users.size());

        for (User user : users) {

            NotificationMessage notificationMessage =
                    NotificationMessage.builder()
                            .type(NotificationConstants.JOB_CREATED)
                            .message(description)
                            .build();

            createUserNotification(user, notification);

            messagingTemplate.convertAndSend(
                    "/topic/notifications/user/" + user.getId(),
                    notificationMessage
            );

            log.info("Notification sent to user {}", user.getId());
        }
        log.info("role base notifications creation successful");
    }

    private Notification createNotification(JobCreatedNotifyEvent jobCreatedNotifyEvent, String description) {
        Notification notification = Notification.builder()
                .description(description)
                .link(baseUrl + "/jobs/" + jobCreatedNotifyEvent.jobId())
                .createdDate(LocalDate.now())
                .createdTime(LocalTime.now())
                .notificationType(NotificationConstants.JOB_CREATED)
                .build();

        return notificationRepository.save(notification);
    }

    private void createUserNotification(User user, Notification notification) {

        //get user notification from userId and type
        UserNotification userNotification = userNotificationRepository.getUserNotificationsListByUserIdAndType(
                user.getId(), NotificationConstants.JOB_CREATED
        );

        //super admins doesn't assigned to a service center
        String userServiceCenter = user.getServiceCenter() != null ? user.getServiceCenter().getName() : null;

        if (userNotification != null) {
            userNotification.setCount(userNotification.getCount() + 1);
            userNotification.setDescription(
                    notificationServiceHelper.notificationDescription(userNotification.getCount() + 1, userServiceCenter));
            userNotificationRepository.save(userNotification);
        } else {
            userNotification = UserNotification.builder()
                    .user(user)
                    .notification(notification)
                    .count(1)
                    .title("New Job Created")
                    .description(notificationServiceHelper.notificationDescription(1, userServiceCenter))
                    .type(NotificationConstants.JOB_CREATED)
                    .build();

            userNotificationRepository.save(userNotification);
        }
    }
}
