package com.flex.api.notifications;

import com.flex.common_module.http.pagination.Pagination;
import com.flex.notification_module.api.services.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/summary")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> notifications(HttpServletRequest request) {
        return notificationService.notifications(request);
    }

    @PutMapping("/all-notifications")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> userNotifications(@RequestBody Pagination pagination, HttpServletRequest request) {
        return notificationService.userNotifications(pagination, request);
    }

    @PutMapping("/user-notification/{userNotificationId}/mark-as-read")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> markAsRead(@PathVariable Integer userNotificationId, HttpServletRequest request) {
        return notificationService.markAsRead(userNotificationId, request);
    }
}
