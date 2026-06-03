package com.flex.notification_module.api.services;

import com.flex.notification_module.impl.entities.NotificationType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface NotificationTypeService {
    ResponseEntity<?> getAll(HttpServletRequest request);
}
