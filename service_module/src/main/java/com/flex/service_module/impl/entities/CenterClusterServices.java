package com.flex.service_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Date;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "center_cluster_services")
public class CenterClusterServices {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "center_cluster_id")
    private CenterCluster centerCluster;
    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;
    private Integer total;
    private Integer downPay;
    private Integer orderNumber;
    private Integer prevOrderNumber;
    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Colombo")
    private LocalTime serviceTime;
    private boolean disabled;
}
