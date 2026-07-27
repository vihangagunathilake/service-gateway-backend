package com.flex.service_module.impl.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/2/2026
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "available_services")
public class AvailableService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "service_point_id")
    private ServicePoint servicePoint;
    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @Transient
    private Integer servicePointId;
    @Transient
    private Integer serviceId;

    public AvailableService(Integer id, Integer servicePointId, Integer serviceId) {
        this.id = id;
        this.servicePointId = servicePointId;
        this.serviceId = serviceId;
    }

    public AvailableService(Integer id, ServicePoint servicePoint, Service service) {
        this.id = id;
        this.servicePoint = servicePoint;
        this.service = service;
    }
}
