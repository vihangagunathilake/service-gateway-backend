package com.flex.notification_module.listeners;

import com.flex.notification_module.impl.entities.NotificationAccess;
import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.repositories.NotificationAccessRepository;
import com.flex.notification_module.impl.repositories.RoleNotificationRepository;
import com.flex.user_module.events.UserCreatedEvent;
import com.flex.user_module.events.UserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationListener {

    private final NotificationAccessRepository notificationAccessRepository;
    private final RoleNotificationRepository roleNotificationRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserCreation(UserCreatedEvent event) {
        log.info("make access to notification for user");
        List<NotificationType> notificationTypes = roleNotificationRepository
                .getNotificationTypesByRole(event.user().getRole().getId());

        if (!notificationTypes.isEmpty()) {
            for (NotificationType notificationType : notificationTypes) {
                NotificationAccess notificationAccess = NotificationAccess.builder()
                        .user(event.user())
                        .notificationType(notificationType)
                        .disabled(true)
                        .build();

                notificationAccessRepository.save(notificationAccess);
            }
        }

        log.info("make access to notification for user successful");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserRoleModification(UserUpdatedEvent event) {
        log.info("edit access to notification for user");
        List<NotificationAccess> existing = notificationAccessRepository
                .findAllByUser(event.user().getId());

        notificationAccessRepository.deleteAll(existing);

        List<NotificationType> notificationTypes = roleNotificationRepository
                .getNotificationTypesByRole(event.user().getRole().getId());

        if (!notificationTypes.isEmpty()) {
            for (NotificationType notificationType : notificationTypes) {
                NotificationAccess notificationAccess = NotificationAccess.builder()
                        .user(event.user())
                        .notificationType(notificationType)
                        .disabled(true)
                        .build();

                notificationAccessRepository.save(notificationAccess);
            }
        }

        log.info("edit access to notification for user successful");
    }
}
