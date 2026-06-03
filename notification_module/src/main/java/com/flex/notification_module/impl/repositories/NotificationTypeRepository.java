package com.flex.notification_module.impl.repositories;

import com.flex.notification_module.impl.entities.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Integer> {
    NotificationType getNotificationTypeByType(String type);

    @Query("SELECT nt FROM NotificationType nt WHERE nt.id in (:ids)")
    List<NotificationType> getAllNotificationTypesById(@Param("ids") List<Integer> ids);
}
