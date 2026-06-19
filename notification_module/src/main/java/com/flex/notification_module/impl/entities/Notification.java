package com.flex.notification_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String description;
    private String link;
    private String notificationType;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate createdDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime createdTime;
}
