package com.flex.notification_module.impl.components;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.constants.NotificationConstants;
import com.flex.job_module.api.http.KafkaEvent;
import com.flex.job_module.api.http.NotificationEvent;
import com.flex.job_module.impl.entities.Job;
import com.flex.notification_module.constants.NotificationDescription;
import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.entities.UserNotification;
import com.flex.notification_module.impl.repositories.NotificationAccessRepository;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class NotificationComponent {

    private final UserRepository userRepository;

    private final NotificationTypeRepository notificationTypeRepository;
    private final NotificationAccessRepository notificationAccessRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void send(ServicePoint servicePoint, Job job, String type, String topic) {

        List<Integer> userIds = userRepository
                .getUserIdsByServiceProvider(servicePoint.getServiceCenter().getServiceProvider().getId());

        // using type find which user has notification access.
        NotificationType notificationType =
                notificationTypeRepository.getNotificationTypeByType(
                        type
                );

        if (notificationType == null) {
            log.warn("Notification type not found");
            return;
        }

        List<User> users =
                notificationAccessRepository.getUsersByIdsAndNotifyType(
                        userIds,
                        notificationType.getId()
                );


        if (users.isEmpty()) {
            log.warn("No users found");
            return;
        }

        NotificationType notification = notificationTypeRepository
                .getNotificationTypeByType(type);

        if (notification == null) {
            log.warn("Notification type not found");
            return;
        }

        for (User user : users) {
            UserNotification userNotification = getUserNotification(user, job, notification, servicePoint);

            userNotificationRepository.save(userNotification);

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(user.getId())
                    .notificationType(type)
                    .build();

            messagingTemplate.convertAndSend(
                    topic + user.getId(),
                    notificationEvent
            );
        }
    }

    public void update(ServicePoint servicePoint, String type, String topic) {

        List<Integer> userIds = userRepository
                .getUserIdsByServiceProvider(servicePoint.getServiceCenter().getServiceProvider().getId());

        // using type find which user has notification access.
        NotificationType notificationType =
                notificationTypeRepository.getNotificationTypeByType(
                        type
                );

        if (notificationType == null) {
            log.warn("Notification type not found");
            return;
        }

        List<User> users =
                notificationAccessRepository.getUsersByIdsAndNotifyType(
                        userIds,
                        notificationType.getId()
                );


        if (users.isEmpty()) {
            log.warn("No users found");
            return;
        }

        NotificationType notification = notificationTypeRepository
                .getNotificationTypeByType(type);

        if (notification == null) {
            log.warn("Notification type not found");
            return;
        }

        for (User user : users) {
            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(user.getId())
                    .notificationType(type)
                    .build();
            messagingTemplate.convertAndSend(
                    topic + user.getId(),
                    notificationEvent
            );
        }
    }

    public void trigger(String topic, Integer userId, KafkaEvent kafkaEvent) {
        log.info("publishing: {} - kafka: {}", topic + userId, kafkaEvent.getEventId());
        messagingTemplate.convertAndSend(
                topic + userId,
                kafkaEvent
        );
    }

    private static UserNotification getUserNotification(User user, Job job, NotificationType notification, ServicePoint servicePoint) {
        UserNotification userNotification = new UserNotification();

        userNotification.setUser(user);
        userNotification.setNotificationType(notification);
        userNotification.setJob(job);
        userNotification.setCreatedDate(CommonMethods.getCurrentDate());
        userNotification.setCreatedTime(CommonMethods.getCurrentTime());
        userNotification.setDescription(NotificationDescription.NO_AGENT
                + servicePoint.getName() + " in " + servicePoint.getServiceCenter().getName());
        userNotification.setTitle(getTitle(notification.getType()));
        userNotification.setServicePoint(servicePoint);
        userNotification.setServiceCenter(servicePoint.getServiceCenter());
        userNotification.setMarkedAsView(false);
        userNotification.setMarkedAsRead(false);
        return userNotification;
    }

    private static String getTitle(String notificationType) {
        if (notificationType.equals(NotificationConstants.NO_AGENT_FOR_JOB)) {
            return NotificationDescription.NO_AGENT_TITLE;
        }

        return "No title";
    }
}
