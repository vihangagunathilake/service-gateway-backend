package com.flex.user_module.impl.repositories;

import com.flex.user_module.impl.entities.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    boolean existsById(Integer id);

    RolePermission findByRoleIdAndPermission_Permission(Integer roleId, String permission);

    @Query("SELECT r FROM RolePermission r WHERE r.role.id=:role AND r.role.deleted = false")
    List<RolePermission> getAllRolePermissions(@Param("role") Integer roleId);

    @Query("SELECT r.permission.permission FROM RolePermission r " +
            "WHERE r.role.id=:role AND r.role.deleted = false")
    List<String> getRolePermissions(@Param("role") Integer roleId);

    @Query("SELECT r FROM RolePermission r " +
            "WHERE r.role.id = :role AND r.permission.permission in (:permissions)")
    List<RolePermission> getRolePermissionsByPermissions(@Param("role") Integer roleId,
                                                         @Param("permissions") List<String> permissions);

}
