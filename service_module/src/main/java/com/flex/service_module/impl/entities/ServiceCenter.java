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
@Table(name = "service_centers")
public class ServiceCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "serviceProviderId")
    private ServiceProvider serviceProvider;
    private String contact;
    private String location;
    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Colombo")
    private LocalTime openTime;
    @JsonFormat(pattern = "HH:mm", timezone = "Asia/Colombo")
    private LocalTime closeTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Colombo")
    @Column(columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    private Date addedTime;
    private boolean deleted;

    public ServiceCenter(Integer id) {
        this.id = id;
    }
}
