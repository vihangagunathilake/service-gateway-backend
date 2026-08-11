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

    UserNotification findByIdAndAlreadySolvedIsFalseAndMarkedAsReadIsFalse(Integer id);

    @Query("SELECT un FROM UserNotification un WHERE un.servicePoint.id=:point " +
            "AND un.notificationType.type=:type " +
            "AND un.markedAsRead = false " +
            "AND un.alreadySolved = false")
    List<UserNotification> getUserNotificationsByUserAndPoint(@Param("point")Integer point,
                                                              @Param("type")String notificationType);

    @Query("SELECT un FROM UserNotification un " +
            "WHERE un.user.id=:userId AND un.notificationType.type in (:types) " +
            "AND un.markedAsView = false AND un.alreadySolved = false " +
            "GROUP BY un.notificationType.type")
    List<UserNotification> getUserNotificationCount(@Param("userId") Integer userId, @Param("types") List<String> types);

    @Query("SELECT un.id FROM UserNotification un " +
            "WHERE un.user.id=:userId " +
            "AND un.markedAsView = false ")
    List<Integer> getNotViewedNotificationIds(@Param("userId") Integer userId);

    @Query("SELECT count(un.id) FROM UserNotification un " +
            "WHERE un.user.id=:userId AND un.createdDate = :date " +
            "AND un.notificationType.type=:type AND un.alreadySolved = false")
    Integer getUserNotificationsCountByUserIdAndType(@Param("userId") Integer userId,
                                                     @Param("type") String type,
                                                     @Param("date") LocalDate date);

    @Query("SELECT un FROM UserNotification un WHERE un.job.id=:jobId AND un.notificationType.id=:notificationType AND un.alreadySolved = false")
    List<UserNotification> getUserNotificationsByJobId(@Param("jobId") Integer jobId, @Param("notificationType") Integer notificationType);

    @Query("SELECT new UserNotification(un.id, un.job.id, un.description, un.createdTime, un.createdDate, un.servicePoint.name, un.serviceCenter.name, un.job.customer.name, un.job.appointmentTime, un.markedAsView, un.markedAsRead) " +
            "FROM UserNotification un WHERE un.createdDate=:date AND un.user.id=:userId " +
            "AND un.notificationType.id=:notificationType AND un.alreadySolved = false")
    List<UserNotification> getNoAgentNotificationsDetailsByUserId(@Param("userId")Integer userId, @Param("notificationType")Integer notificationType,
                                                                  @Param("date") LocalDate date);

    @Query("SELECT un " +
            "FROM UserNotification un WHERE un.user.id=:userId " +
            "AND un.notificationType.id=:notificationType AND un.alreadySolved = false")
    List<UserNotification> getNoAgentNotificationsByUserId(@Param("userId")Integer userId,
                                                                  @Param("notificationType")Integer notificationType);

    @Modifying
    @Transactional
    @Query("UPDATE UserNotification un SET un.markedAsView = true " +
            "WHERE un.id in (:unIds) AND un.notificationType.type not in (:types)")
    void markAsViewButNotForCrucial(@Param("unIds") List<Integer> unIds, @Param("types") List<String> types);
}
