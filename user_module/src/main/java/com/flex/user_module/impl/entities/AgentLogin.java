package com.flex.user_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServicePoint;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "agent_login")
public class AgentLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "service_point_id")
    private ServicePoint servicePoint;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate loginDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime loginTime;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate logoutDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime logoutTime;

}
