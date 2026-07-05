package com.flex.user_module.impl.repositories;

import com.flex.user_module.impl.entities.Permission;
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
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    @Query("SELECT p.permission FROM Permission p WHERE p.id in (:ids)")
    List<String> getPermissionsByIds(@Param("ids") List<Integer> ids);

    @Query("SELECT p.permission FROM Permission p WHERE p.employee = false")
    List<String> getPermissions();

    @Query("SELECT p FROM Permission p WHERE p.permission in (:permissions)")
    List<Permission> getAlPermissionsByPermission(@Param("permissions") List<String> permissions);

    List<Permission> getAllByEmployeeIsTrue();

    Permission getPermissionByPermission(String permission);
}
