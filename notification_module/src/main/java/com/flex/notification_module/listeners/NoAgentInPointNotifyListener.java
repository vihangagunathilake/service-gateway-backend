package com.flex.notification_module.listeners;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.constants.NotificationConstants;
import com.flex.job_module.api.http.NotificationEvent;
import com.flex.job_module.events.NoAgentInPointEvent;
import com.flex.notification_module.constants.NotificationDescription;
import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.entities.UserNotification;
import com.flex.notification_module.impl.repositories.NotificationAccessRepository;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.notification_module.kafka.events.publishers.NoAgentInPointPublisher;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.repositories.ServicePointRepository;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class NoAgentInPointNotifyListener {

    private final ServicePointRepository servicePointRepository;
    private final UserRepository userRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final NotificationAccessRepository notificationAccessRepository;
    private final UserNotificationRepository userNotificationRepository;

    private final NoAgentInPointPublisher noAgentInPointPublisher;

    @Value("${app.frontend.url}")
    private String baseUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationsToManagement(NoAgentInPointEvent noAgentInPointEvent) {

        // get the service center from point.
        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(noAgentInPointEvent.pointId());

        if (servicePoint == null) {
            log.warn("Service point not found: {} ", noAgentInPointEvent.pointId());
            return;
        }

        // get all users/admin ids from service provider
        List<Integer> userIds = userRepository
                .getUserIdsByServiceProvider(servicePoint.getServiceCenter().getServiceProvider().getId());

        // using type find which user has notification access.
        NotificationType notificationType =
                notificationTypeRepository.getNotificationTypeByType(
                        NotificationConstants.NO_AGENT_FOR_JOB
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
                .getNotificationTypeByType(NotificationConstants.NO_AGENT_FOR_JOB);

        if (notification == null) {
            log.warn("Notification type not found");
            return;
        }

        for (User user : users) {
            UserNotification userNotification = getUserNotification(user, notification, servicePoint);

            userNotificationRepository.save(userNotification);

            NotificationEvent notificationEvent = NotificationEvent.builder()
                    .userId(user.getId())
                    .notificationType(NotificationConstants.NO_AGENT_FOR_JOB)
                    .build();

            noAgentInPointPublisher.publish(notificationEvent);
        }
    }

    private static UserNotification getUserNotification(User user, NotificationType notification, ServicePoint servicePoint) {
        UserNotification userNotification = new UserNotification();

        userNotification.setUser(user);
        userNotification.setNotificationType(notification);
        userNotification.setCreatedDate(CommonMethods.getCurrentDate());
        userNotification.setCreatedTime(CommonMethods.getCurrentTime());
        userNotification.setDescription(NotificationDescription.NO_AGENT
                + servicePoint.getName() + " in " + servicePoint.getServiceCenter().getName());
        userNotification.setMarkedAsView(false);
        userNotification.setMarkedAsRead(false);
        return userNotification;
    }
}
