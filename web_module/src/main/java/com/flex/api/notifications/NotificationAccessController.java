package com.flex.api.notifications;

import com.flex.common_module.http.pagination.Pagination;
import com.flex.notification_module.api.http.requests.AssignNotification;
import com.flex.notification_module.api.services.NotificationAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification-access")
@RequiredArgsConstructor
public class NotificationAccessController {

    private final NotificationAccessService notificationAccessService;

    @GetMapping("/user/{uid}/get-all")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> notifications(@PathVariable("uid") Integer uid, HttpServletRequest request) {
        return notificationAccessService.getAllByUser(uid, request);
    }

    @PutMapping("/assign")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> userNotifications(@RequestBody AssignNotification assignNotification, HttpServletRequest request) {
        return notificationAccessService.assignNotifications(assignNotification, request);
    }

    @GetMapping("/user-assigned")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> userOwnNotifications(HttpServletRequest request) {
        return notificationAccessService.userNotifications(request);
    }
}
