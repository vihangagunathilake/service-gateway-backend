package com.flex.user_module.impl.entities;

import com.flex.service_module.impl.entities.ServiceProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String role;
    @ManyToOne
    @JoinColumn(name = "serviceProviderId")
    private ServiceProvider serviceProvider;
    private boolean restricted;
    private boolean employee;
    private boolean deleted;

    public Role(Integer id) {
        this.id = id;
    }
}
