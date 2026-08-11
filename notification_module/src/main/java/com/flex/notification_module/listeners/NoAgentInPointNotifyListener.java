package com.flex.notification_module.listeners;
import com.flex.common_module.constants.NotificationConstants;
import com.flex.job_module.events.NoAgentInPointEvent;
import com.flex.notification_module.impl.components.NotificationComponent;
import com.flex.notification_module.kafka.topics.KafkaNotificationTopics;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.repositories.ServicePointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    private final NotificationComponent notificationComponent;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationsToManagement(NoAgentInPointEvent noAgentInPointEvent) {

        // get the service center from point.
        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(noAgentInPointEvent.pointId());

        if (servicePoint == null) {
            log.warn("Service point not found: {} ", noAgentInPointEvent.pointId());
            return;
        }

        notificationComponent.send(servicePoint, noAgentInPointEvent.job(),
                NotificationConstants.NO_AGENT_FOR_JOB,
                KafkaNotificationTopics.NO_AGENT_IN_POINT_TOPIC);
    }
}
