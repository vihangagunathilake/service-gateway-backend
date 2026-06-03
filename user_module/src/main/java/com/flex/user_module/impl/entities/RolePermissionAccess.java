package com.flex.user_module.impl.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "role_permission_access")
public class RolePermissionAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private boolean add_permission;
    private boolean update_permission;
    private boolean delete_permission;
    private boolean getAll_permission;
    private boolean get_permission;
    private boolean assign_permission;
    private boolean all_permission;

    @ManyToOne()
    @JoinColumn(name = "rolePermissionId", nullable = false)
    private RolePermission rolePermission;
}
