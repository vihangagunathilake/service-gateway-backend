package com.flex.notification_module.listeners;

import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.entities.RoleNotification;
import com.flex.notification_module.impl.repositories.NotificationAccessRepository;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import com.flex.notification_module.impl.repositories.RoleNotificationRepository;
import com.flex.user_module.events.RoleCreatedEvent;
import com.flex.user_module.events.RoleDeleteEvent;
import com.flex.user_module.events.RoleUpdateEvent;
import com.flex.user_module.impl.entities.Role;
import com.flex.user_module.impl.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

//This class handles notification action changes based on role modification

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleNotificationListener {
    private final RoleNotificationRepository roleNotificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRoleCreated(RoleCreatedEvent event) {
        log.info("role base notifications creation started");
        List<NotificationType> notificationTypes = notificationTypeRepository
                .getAllNotificationTypesById(event.notificationTypesIds());

        for (NotificationType notificationType : notificationTypes) {
            RoleNotification roleNotification = RoleNotification.builder()
                    .role(new Role(event.roleId()))
                    .notificationType(notificationType)
                    .build();
            roleNotificationRepository.save(roleNotification);
        }
        log.info("role base notifications creation successful");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRoleUpdated(RoleUpdateEvent event) {
        log.info("role base notifications modification started");

        List<NotificationType> notificationTypes = notificationTypeRepository
                .getAllNotificationTypesById(event.notificationTypesIds());

        List<RoleNotification> existing = roleNotificationRepository
                .findAllByRoleId(event.roleId());

        if (!existing.isEmpty()) {
            roleNotificationRepository.deleteAll(existing);
        }

        for (NotificationType notificationType : notificationTypes) {
            RoleNotification roleNotification = RoleNotification.builder()
                    .role(new Role(event.roleId()))
                    .notificationType(notificationType)
                    .build();

            roleNotificationRepository.save(roleNotification);
        }

        log.info("role base notifications modification successful");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRoleDeleted(RoleDeleteEvent event) {
        log.info("role base notifications deletion started");

        List<RoleNotification> existing = roleNotificationRepository
                .findAllByRoleId(event.roleId());

        if (!existing.isEmpty()) {
            roleNotificationRepository.deleteAll(existing);
        }

        log.info("role base notifications deletion successful");
    }
}
