package com.flex.notification_module.impl.services.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceHelper {

    public String notificationDescription(int count, String serviceCenter) {
        if (count <= 0) {
            return null;
        } else if (count == 1) {
            if (serviceCenter != null) {
                return "new job created in " + serviceCenter;
            } else {
                return "new job created.";
            }
        } else {
            if (serviceCenter != null) {
                return "new jobs created in " + serviceCenter;
            } else {
                return "new jobs created.";
            }
        }
    }
}
