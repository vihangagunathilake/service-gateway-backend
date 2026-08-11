package com.flex.notification_module.kafka.events.listeners.agent;

import com.flex.common_module.constants.NotificationConstants;
import com.flex.job_module.api.http.KafkaEvent;
import com.flex.job_module.events.kafka.CustomerArrivedTrigger;
import com.flex.notification_module.constants.KafkaEvents;
import com.flex.notification_module.impl.components.NotificationComponent;
import com.flex.notification_module.kafka.topics.KafkaNotificationTopics;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.repositories.ServicePointRepository;
import com.flex.user_module.impl.entities.AgentLogin;
import com.flex.user_module.impl.repositories.AgentLoginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class CustomerArrivedListener {

    private final ServicePointRepository servicePointRepository;
    private final AgentLoginRepository agentLoginRepository;

    private final NotificationComponent notificationComponent;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerKafkaMessage(CustomerArrivedTrigger customerArrivedTrigger) {
        // get the service center from point.
        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(customerArrivedTrigger.servicePoint());

        if (servicePoint == null) {
            log.warn("Service point not found: {} ", customerArrivedTrigger.servicePoint());
            return;
        }

        AgentLogin agentLogin = agentLoginRepository.getAgentLoginById(customerArrivedTrigger.loginAgentId());

        if (agentLogin == null) {
            log.warn("Agent login not found: {} ", customerArrivedTrigger.loginAgentId());
            return;
        }

        Integer userId = agentLogin.getUser().getId();

        KafkaEvent kafkaEvent = KafkaEvent.builder()
                .eventId(KafkaEvents.CUSTOMER_ARRIVED)
                .build();

        notificationComponent.trigger(KafkaNotificationTopics.CUSTOMER_ARRIVED_TOPIC, userId, kafkaEvent);
    }
}
