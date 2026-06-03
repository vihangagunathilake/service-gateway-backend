package com.flex.notification_module.impl.repositories;

import com.flex.notification_module.impl.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("SELECT j FROM Notification j ORDER BY j.createdDate desc , j.createdTime desc")
    Page<Notification> notifications(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT j FROM Notification j ORDER BY j.createdDate desc , j.createdTime desc limit 10")
    List<Notification> findLatestNotifications(@Param("userId") Integer userId);
}
