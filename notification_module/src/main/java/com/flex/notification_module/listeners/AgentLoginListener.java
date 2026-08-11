package com.flex.notification_module.listeners;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.constants.NotificationConstants;
import com.flex.notification_module.impl.components.NoAgentNotification;
import com.flex.notification_module.impl.components.NotificationComponent;
import com.flex.notification_module.impl.entities.UserNotification;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.notification_module.kafka.topics.KafkaNotificationTopics;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.repositories.ServicePointRepository;
import com.flex.user_module.events.AgentLoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class AgentLoginListener {

    private final NotificationComponent notificationComponent;
    private final NoAgentNotification noAgentNotification;

    private final ServicePointRepository servicePointRepository;

    private final UserNotificationRepository userNotificationRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkNoAgentAtPointSolved(AgentLoginEvent event) {

        if (event.servicePoint() == null) {
            return;
        }

        List<UserNotification> userNotifications = userNotificationRepository.getUserNotificationsByUserAndPoint(
                event.servicePoint().getId(),
                NotificationConstants.NO_AGENT_FOR_JOB
        );

        noAgentNotification.solveNoAgentNotification(event.job());

        if (!userNotifications.isEmpty()) {
            for (UserNotification userNotification : userNotifications) {
                userNotification.setAlreadySolved(true);
                userNotification.setSolvedDate(CommonMethods.getCurrentDate());
                userNotification.setSolvedTime(CommonMethods.getCurrentTime());
                userNotificationRepository.save(userNotification);
            }
            notificationComponent.update(event.servicePoint(),
                    NotificationConstants.NO_AGENT_FOR_JOB,
                    KafkaNotificationTopics.NO_AGENT_IN_POINT_TOPIC);
        }
    }
}
