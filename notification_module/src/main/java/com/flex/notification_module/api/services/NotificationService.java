package com.flex.notification_module.api.services;

import com.flex.common_module.http.pagination.Pagination;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface NotificationService {
    // capture the kafka message and show the notification count on bell icon
    ResponseEntity<?> notify(HttpServletRequest request);

    ResponseEntity<?> notifyNoAgent(HttpServletRequest request);

    // to disappear notification count when click on bell icon
    ResponseEntity<?> notified(HttpServletRequest request);

    ResponseEntity<?> timeoutJobs(HttpServletRequest request);

    ResponseEntity<?> noAgentNotifications(HttpServletRequest request);

    ResponseEntity<?> noAgentMarkAsView(HttpServletRequest request);

    ResponseEntity<?> noAgentMarkAsRead(Integer notificationId, HttpServletRequest request);
}
