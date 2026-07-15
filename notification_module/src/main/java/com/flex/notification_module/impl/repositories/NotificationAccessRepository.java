package com.flex.notification_module.impl.repositories;

import com.flex.notification_module.api.http.DTO.NotificationAccessList;
import com.flex.notification_module.impl.entities.NotificationAccess;
import com.flex.notification_module.impl.entities.NotificationType;
import com.flex.user_module.impl.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationAccessRepository extends JpaRepository<NotificationAccess, Integer> {
    List<NotificationAccess> findAllByUserId(Integer userId);

    @Query("SELECT na.notificationType.id as accessId, na.notificationType.type as accessName " +
            "FROM NotificationAccess na WHERE na.user.id=:userId AND na.disabled = false")
    List<NotificationAccessList> findAccessListByUserId(@Param("userId") Integer userId);

    @Query("SELECT na FROM NotificationAccess na WHERE na.user.id=:userId AND na.user.deleted = false")
    List<NotificationAccess> findAllByUser(@Param("userId") Integer userId);

    @Query("SELECT na.notificationType FROM NotificationAccess na WHERE na.user.id=:userId AND na.user.deleted = false")
    List<NotificationType> findAllTypesByUser(@Param("userId") Integer userId);

    @Query("SELECT na.user FROM NotificationAccess na " +
            "WHERE na.user.serviceCenter.id=:center " +
            "AND na.notificationType.id=:type " +
            "AND na.disabled = false AND na.user.deleted = false")
    List<User> getUsersByTypeAndServiceProvider(@Param("type") Integer type,
                                                @Param("center") Integer center);

    @Query("SELECT na.user FROM NotificationAccess na " +
            "WHERE na.user.id in :userIds " +
            "AND na.notificationType.id=:type " +
            "AND na.disabled = false")
    List<User> getUsersByIdsAndNotifyType(@Param("userIds") List<Integer> userIds,
                                                @Param("type") Integer type);

}
