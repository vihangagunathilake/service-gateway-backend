package com.flex.notification_module.api.services;

import com.flex.notification_module.api.http.requests.AssignNotification;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface NotificationAccessService {
    ResponseEntity<?> assignNotifications(AssignNotification assignNotification, HttpServletRequest request);

    ResponseEntity<?> getAllByUser(Integer userId, HttpServletRequest request);

    ResponseEntity<?> userNotifications(HttpServletRequest request);

    ResponseEntity<?> roleNotifications(HttpServletRequest request);

}
