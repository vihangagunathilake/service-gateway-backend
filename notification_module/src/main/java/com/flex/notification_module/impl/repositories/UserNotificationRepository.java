package com.flex.notification_module.impl.repositories;

import com.flex.notification_module.api.http.DTO.UserNotificationsList;
import com.flex.notification_module.impl.entities.Notification;
import com.flex.notification_module.impl.entities.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {

    UserNotification getUserNotificationByIdAndViewedIsFalse(Integer id);

    @Query("SELECT un FROM UserNotification un " +
            "WHERE un.user.id=:userId " +
            "AND un.notification.notificationType=:type AND un.viewed = false " +
            "ORDER BY un.notification.createdDate desc, un.notification.createdTime desc")
    UserNotification getUserNotificationsListByUserIdAndType(@Param("userId") Integer userId, @Param("type") String type);

    @Query("SELECT un.id as id , un.notification.notificationType as type, un.count as count, " +
            "un.description as description, " +
            "un.link as link, " +
            "un.viewed as read " +
            "FROM UserNotification un " +
            "WHERE un.user.id=:userId " +
            "AND un.notification.notificationType=:type " +
            "ORDER BY un.notification.createdDate desc, un.notification.createdTime desc")
    List<UserNotificationsList> getUserNotificationsListByUserId(@Param("userId") Integer userId, @Param("type") String type);
}
