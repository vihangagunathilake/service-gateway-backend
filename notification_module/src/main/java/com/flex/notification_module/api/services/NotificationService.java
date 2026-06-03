package com.flex.notification_module.api.services;

import com.flex.common_module.http.pagination.Pagination;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface NotificationService {
    ResponseEntity<?> notifications(HttpServletRequest request);

    ResponseEntity<?> userNotifications(Pagination pagination, HttpServletRequest request);

    ResponseEntity<?> markAsRead(Integer notificationId, HttpServletRequest request);
}
