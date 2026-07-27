package com.flex.job_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flex.service_module.impl.entities.Service;
import com.flex.service_module.impl.entities.ServicePoint;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/12/2026
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "jobs_at_point")
public class JobAtPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "service_point_id")
    private ServicePoint servicePoint;
    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;
    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    @Column(nullable = false)
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    @Column(nullable = false)
    private LocalTime endTime;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime actualEndTime;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate createdDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    @Column(nullable = false)
    private LocalTime createdTime;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime customerArrivedTime;

    private int status;

    private boolean dummyEntity;
    private boolean allowToServe;

    @Transient
    private Integer mainJobId;
    @Transient
    private Integer serviceDownPrice;
    @Transient
    private Integer serviceTotalPrice;

    public JobAtPoint(Integer mainJobId, LocalTime startTime, LocalTime endTime) {
        this.mainJobId = mainJobId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public JobAtPoint(Integer id, Integer serviceDownPrice, Integer serviceTotalPrice) {
        this.id = id;
        this.serviceDownPrice = serviceDownPrice;
        this.serviceTotalPrice = serviceTotalPrice;
    }
}
