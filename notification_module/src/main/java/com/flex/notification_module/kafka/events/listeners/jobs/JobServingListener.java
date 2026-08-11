package com.flex.notification_module.kafka.events.listeners.jobs;

import com.flex.job_module.api.http.KafkaEvent;
import com.flex.notification_module.constants.KafkaEvents;
import com.flex.notification_module.impl.components.NotificationComponent;
import com.flex.notification_module.kafka.topics.KafkaNotificationTopics;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.ServiceCenterRepository;
import com.flex.service_module.impl.repositories.ServiceProviderRepository;
import com.flex.user_module.api.DTO.CenterUsers;
import com.flex.user_module.constants.UserConstant;
import com.flex.user_module.events.JobServingEvent;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.entities.UserLogin;
import com.flex.user_module.impl.repositories.UserLoginRepository;
import com.flex.user_module.impl.repositories.UserRepository;
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
public class JobServingListener {

    private final ServiceCenterRepository serviceCenterRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;

    private final NotificationComponent notificationComponent;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerKafkaMessage(JobServingEvent jobServingEvent) {

        ServiceProvider serviceProvider = serviceProviderRepository
                .findByIdAndDeletedIsFalse(jobServingEvent.serviceProvider());

        if (serviceProvider == null) {
            log.warn("service provider not found");
            return;
        }

        List<Integer> userIds = userRepository
                .getUserIdsByProviderIdButNotUserType(serviceProvider.getId(), UserConstant.EMPLOYEE);

        log.info("userIds ----> {}", userIds);

        if (userIds != null && !userIds.isEmpty()) {

            List<Integer> loginUserIds = userLoginRepository.getAllLoginUsers(userIds);

            log.info("logged In userIds ----> {}", loginUserIds);

            for (Integer loginUserId : loginUserIds) {

                KafkaEvent kafkaEvent = KafkaEvent.builder()
                        .eventId(KafkaEvents.JOB_SERVING)
                        .build();

                notificationComponent.trigger(KafkaNotificationTopics.JOB_SERVING_TOPIC, loginUserId, kafkaEvent);
            }
        }
    }
}
