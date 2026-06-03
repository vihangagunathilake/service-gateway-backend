package com.flex.user_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServiceProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String fName;
    private String lName;
    private String email;
    private String password;
    private String image;
    //0-user, 1-admin, 2-customer
    private int userType;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider serviceProvider;
    @ManyToOne
    @JoinColumn(name = "service_center_id")
    private ServiceCenter serviceCenter;
    private boolean deleted;

    public User(Integer id) {
        this.id = id;
    }
}
