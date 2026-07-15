package com.flex.notification_module.impl.services;

import com.flex.common_module.http.ReturnResponse;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.notification_module.api.http.requests.AssignNotification;
import com.flex.notification_module.api.http.responses.UserNotifications;
import com.flex.notification_module.api.services.NotificationAccessService;
import com.flex.notification_module.impl.entities.NotificationAccess;
import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.repositories.NotificationAccessRepository;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import com.flex.notification_module.impl.repositories.RoleNotificationRepository;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.flex.common_module.http.ReturnResponse.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class NotificationAccessServiceImpl implements NotificationAccessService {

    private final NotificationAccessRepository notificationAccessRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final UserRepository userRepository;
    private final RoleNotificationRepository roleNotificationRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Override
    public ResponseEntity<?> assignNotifications(AssignNotification assignNotification, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User claims not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        List<NotificationAccess> existingNotificationAccess = notificationAccessRepository
                .findAllByUserId(user.getId());

        if (!existingNotificationAccess.isEmpty()) {
            notificationAccessRepository.deleteAll(existingNotificationAccess);
        }

        List<NotificationType> notificationTypes = notificationTypeRepository
                .getAllNotificationTypesById(assignNotification.getNotificationTypes());

        if (!notificationTypes.isEmpty()) {

            List<NotificationAccess> notificationAccesses = new ArrayList<>();

            for (NotificationType notificationType : notificationTypes) {
                NotificationAccess notificationAccess = NotificationAccess.builder()
                        .user(user)
                        .notificationType(notificationType)
                        .build();

                notificationAccesses.add(notificationAccess);
            }

            notificationAccessRepository.saveAll(notificationAccesses);

            return SUCCESS("Successfully assigned notifications");
        }

        return SUCCESS("No notifications found");
    }

    @Override
    public ResponseEntity<?> getAllByUser(Integer userId, HttpServletRequest request) {
        log.info(request.getRequestURI());
        return DATA(notificationAccessRepository.findAccessListByUserId(userId));
    }

    @Override
    public ResponseEntity<?> userNotifications(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        // get types by role
        List<NotificationType> notificationTypes = roleNotificationRepository
                .getNotificationTypesByRole(user.getRole().getId());

        if (notificationTypes.isEmpty()) {
            return SUCCESS(null);
        }

        List<UserNotifications> userNotifications = new ArrayList<>();

        // get access by user
        List<NotificationType> notificationTypesByUser = notificationAccessRepository
                .findAllTypesByUser(user.getId());

        for (NotificationType notificationType : notificationTypes) {
            UserNotifications userNotification;
            if (notificationTypesByUser.contains(notificationType)) {
                userNotification = UserNotifications.builder()
                        .typeId(notificationType.getId())
                        .title(notificationType.getName())
                        .content(notificationType.getDescription())
                        .disabled(false)
                        .build();
            } else {
                userNotification = UserNotifications.builder()
                        .typeId(notificationType.getId())
                        .title(notificationType.getName())
                        .content(notificationType.getDescription())
                        .disabled(true)
                        .build();
            }
            userNotifications.add(userNotification);
        }

        return DATA(userNotifications);
    }

    @Override
    public ResponseEntity<?> roleNotifications(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        return DATA(roleNotificationRepository.getRoleNotificationTypes(user.getRole().getId()));
    }
}
