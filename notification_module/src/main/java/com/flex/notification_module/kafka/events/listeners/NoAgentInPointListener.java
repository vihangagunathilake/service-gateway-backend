package com.flex.notification_module.kafka.events.listeners;

import com.flex.job_module.api.http.NotificationEvent;
import com.flex.notification_module.kafka.topics.KafkaNotificationTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoAgentInPointListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handle(NotificationEvent event) {
        log.info("kafka message publish for {}", KafkaNotificationTopics.NO_AGENT_IN_POINT_TOPIC + event.getUserId());
        messagingTemplate.convertAndSend(
                KafkaNotificationTopics.NO_AGENT_IN_POINT_TOPIC + event.getUserId(),
                event
        );
    }
}
