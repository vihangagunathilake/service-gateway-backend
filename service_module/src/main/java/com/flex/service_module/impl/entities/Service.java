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
 * @since 2/2/2026
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "services")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider provider;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime serviceTime;
    @Column(length = 512)
    private String description;
    private Integer orderNumber;
    private Integer totalPrice;
    private boolean freeService;
    private Integer downPrice;
    // this means 'delete this point at 11:59:00 pm when the last job is ended'
    // this become true when user delete the point even has assigned but pending
    // jobs
    // do not load these kind service points for customer
    // load these kind service points for admin, but not allow any modifications
    private boolean noLongerAvailable;
    private boolean deleted;

    public Service(Integer id) {
        this.id = id;
    }
}
