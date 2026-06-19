package com.flex.service_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/3/2026
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "service_point")
public class ServicePoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String shortName;
    @ManyToOne
    @JoinColumn(name = "service_center_id")
    private ServiceCenter serviceCenter;
    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Colombo")
    private LocalTime openTime;
    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Colombo")
    private LocalTime closeTime;
    private boolean temporaryClosed;
    //this means 'delete this point at 11:59:00 pm when the last job is ended'
    //this become true when user delete the point even has assigned but pending jobs
    //do not load these kind service points for customer
    //load these kind service points for admin, but not allow any modifications
    private boolean noLongerAvailable;
    private boolean deleted;

    @Transient
    private Integer serviceCenterId;
    @Transient
    private String serviceCenterName;
    @Transient
    private Long serviceCount;

    public ServicePoint(Integer id) {
        this.id = id;
    }

    public ServicePoint(Integer id, String name, String shortName, LocalTime openTime, LocalTime closeTime, boolean temporaryClosed,
                        boolean deleted, Integer serviceCenterId, String serviceCenterName, Long serviceCount) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.temporaryClosed = temporaryClosed;
        this.deleted = deleted;
        this.serviceCenterId = serviceCenterId;
        this.serviceCenterName = serviceCenterName;
        this.serviceCount = serviceCount;
    }
}
