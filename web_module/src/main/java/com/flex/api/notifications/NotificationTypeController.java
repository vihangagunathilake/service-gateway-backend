package com.flex.api.notifications;

import com.flex.notification_module.api.services.NotificationTypeService;
import com.flex.notification_module.impl.entities.NotificationType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification-types")
@RequiredArgsConstructor
public class NotificationTypeController {

    private final NotificationTypeService notificationTypeService;

    @GetMapping("/get-all")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NM)")
    public ResponseEntity<?> getAll(HttpServletRequest request) {
        return notificationTypeService.getAll(request);
    }

}
