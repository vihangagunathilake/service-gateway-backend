package com.flex.service_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "service_providers")
public class ServiceProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, nullable = false, length = 8)
    private String providerId;
    private String name;
    private String email;
    private String contact;
    private String address;
    private String website;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Colombo")
    @Column(columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    private Date addedTime;
    @Column(length = 1000)
    private String description;
    private Integer serviceFee;
    private String profileImage;
    private String coverPhoto;
    private boolean active;
    private boolean hasMultipleCenters;
    private boolean restricted;
    private boolean deleted;
}
