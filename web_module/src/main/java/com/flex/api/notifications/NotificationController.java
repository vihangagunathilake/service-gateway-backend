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

    @GetMapping("/notify")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> notify(HttpServletRequest request) {
        return notificationService.notify(request);
    }

    @GetMapping("/notified")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> notified(HttpServletRequest request) {
        return notificationService.notified(request);
    }

    @GetMapping("/notify-no-agent")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> notifyNoAgent(HttpServletRequest request) {
        return notificationService.notifyNoAgent(request);
    }

    @GetMapping("/timeout-jobs-count")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> timeoutJobs(HttpServletRequest request) {
        return notificationService.timeoutJobs(request);
    }

    @GetMapping("/get-no-agent")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> noAgentNotifications(HttpServletRequest request) {
        return notificationService.noAgentNotifications(request);
    }

    @GetMapping("/no-agent-mark-as-view")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> noAgentMarkAsView(HttpServletRequest request) {
        return notificationService.noAgentMarkAsView(request);
    }

    @GetMapping("/no-agent-mark-as-read/notification/{id}")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).NP)")
    public ResponseEntity<?> noAgentMarkAsRead(@PathVariable("id") Integer id, HttpServletRequest request) {
        return notificationService.noAgentMarkAsRead(id, request);
    }
}
