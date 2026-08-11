package com.flex.notification_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flex.common_module.CommonMethods;
import com.flex.job_module.impl.entities.Job;
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
    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;
    private String description;
    private String title;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate createdDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime createdTime;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate solvedDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime solvedTime;

    private boolean markedAsView;
    private boolean markedAsRead;
    private boolean alreadySolved;

    @Transient
    private Integer jobIdInt;
    @Transient
    private String jobId;
    @Transient
    private String formattedTime;
    @Transient
    private String servicePointName;
    @Transient
    private String serviceCenterName;
    @Transient
    private String customerName;
    @Transient
    private LocalTime expectedStartTime;
    @Transient
    private String expectedStartTimeFormatted;

    public UserNotification(Integer id, Integer jobIdInt, String description, LocalTime createdTime, LocalDate createdDate,
                            String servicePointName, String serviceCenterName, String customerName, LocalTime expectedStartTime, boolean markedAsView, boolean markedAsRead) {
        this.id = id;
        this.jobId = "JOB-"+ jobIdInt;
        this.description = description;
        this.createdTime = createdTime;
        this.formattedTime = CommonMethods.timeFormat(createdTime);
        this.createdDate = createdDate;
        this.servicePointName = servicePointName;
        this.serviceCenterName = serviceCenterName;
        this.customerName = customerName;
        this.expectedStartTimeFormatted = CommonMethods.timeFormat(expectedStartTime);
        this.markedAsView = markedAsView;
        this.markedAsRead = markedAsRead;
    }
}
