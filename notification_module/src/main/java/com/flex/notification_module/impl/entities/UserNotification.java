package com.flex.notification_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.user_module.impl.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_notifications")
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "notification_type_id")
    private NotificationType notificationType;
    @ManyToOne
    @JoinColumn(name = "service_point_id")
    private ServicePoint servicePoint;
    @ManyToOne
    @JoinColumn(name = "service_center_id")
    private ServiceCenter serviceCenter;
    private String description;
    private String title;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate createdDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime createdTime;

    private boolean markedAsView;
    private boolean markedAsRead;
}
