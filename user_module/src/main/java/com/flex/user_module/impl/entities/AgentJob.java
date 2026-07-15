package com.flex.user_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.service_module.impl.entities.ServicePoint;
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
@Table(name = "agent_jobs")
public class AgentJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "agent_id")
    private User agent;
    @ManyToOne
    @JoinColumn(name = "job_at_point_id")
    private JobAtPoint jobAtPoint;
    @ManyToOne
    @JoinColumn(name = "point_id")
    private ServicePoint servicePoint;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate addedDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime endTime;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    @Column(nullable = false)
    private LocalTime expectedStartTime;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    @Column(nullable = false)
    private LocalTime expectedEndTime;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime customerArrivedTime;
    private long expectedDuration;
    private long duration;
    private boolean durationMatched; // duration / expectedDuration * 100 > 75%
    private int status;
}
