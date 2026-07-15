package com.flex.notification_module.impl.repositories;

import com.flex.notification_module.api.http.DTO.UserNotificationsList;
import com.flex.notification_module.impl.entities.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {

    @Query("SELECT un FROM UserNotification un " +
            "WHERE un.user.id=:userId AND un.notificationType.type in (:types) " +
            "AND un.markedAsView = false " +
            "GROUP BY un.notificationType.type")
    List<UserNotification> getUserNotificationCount(@Param("userId") Integer userId, @Param("types") List<String> types);

    @Query("SELECT un.id FROM UserNotification un " +
            "WHERE un.user.id=:userId " +
            "AND un.markedAsView = false ")
    List<Integer> getNotViewedNotificationIds(@Param("userId") Integer userId);

    @Query("SELECT count(un.id) FROM UserNotification un " +
            "WHERE un.user.id=:userId AND un.createdDate = :date " +
            "AND un.notificationType.type=:type AND un.markedAsView = false ")
    Integer getUserNotificationsCountByUserIdAndType(@Param("userId") Integer userId,
                                                     @Param("type") String type,
                                                     @Param("date") LocalDate date);

    @Query("SELECT un FROM UserNotification un " +
            "WHERE un.user.id=:userId " +
            "AND un.notificationType.type=:type AND un.markedAsView = false ")
    UserNotification getUserNotificationsListByUserIdAndType(@Param("userId") Integer userId, @Param("type") String type);

    @Query("SELECT un.id FROM UserNotification un " +
            "WHERE un.user.id=:userId AND un.notificationType.type=:type " +
            "AND un.markedAsView = false ")
    List<Integer> getNotViewedNotificationIdsByType(@Param("userId") Integer userId, @Param("type") String type);

    @Query("SELECT un.id as id , " +
            "un.notificationType as type, " +
            "un.description as description, " +
            "un.markedAsView as read " +
            "FROM UserNotification un " +
            "WHERE un.user.id=:userId " +
            "AND un.notificationType=:type " +
            "ORDER BY un.createdDate desc, un.createdTime desc")
    List<UserNotificationsList> getUserNotificationsListByUserId(@Param("userId") Integer userId, @Param("type") String type);

    @Modifying
    @Transactional
    @Query("UPDATE UserNotification un SET un.markedAsView = true " +
            "WHERE un.id in (:unIds) AND un.notificationType.type not in (:types)")
    void markAsViewButNotForCrucial(@Param("unIds") List<Integer> unIds, @Param("types") List<String> types);
}
