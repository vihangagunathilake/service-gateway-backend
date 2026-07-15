package com.flex.notification_module.kafka.events.publishers;

import com.flex.job_module.api.http.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoAgentInPointPublisher {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void publish(NotificationEvent event) {
        publisher.publishEvent(event);
    }
}
