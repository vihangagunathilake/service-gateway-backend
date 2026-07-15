package com.flex.notification_module.impl.repositories;

import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.notification_module.impl.entities.RoleNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleNotificationRepository extends JpaRepository<RoleNotification, Integer> {

    @Query("SELECT rn FROM RoleNotification rn WHERE rn.role.id=:roleId")
    List<RoleNotification> findAllByRoleId(@Param("roleId") Integer roleId);

    @Query("SELECT rn.notificationType FROM RoleNotification rn WHERE rn.role.id=:roleId")
    List<NotificationType> getNotificationTypesByRole(@Param("roleId") Integer roleId);

    @Query("SELECT rn.notificationType.type FROM RoleNotification rn WHERE rn.role.id=:roleId")
    List<String> getRoleNotificationTypes(@Param("roleId") Integer roleId);
}
