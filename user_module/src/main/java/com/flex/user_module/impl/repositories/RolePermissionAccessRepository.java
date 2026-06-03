package com.flex.user_module.impl.repositories;

import com.flex.user_module.impl.entities.RolePermission;
import com.flex.user_module.impl.entities.RolePermissionAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RolePermissionAccessRepository extends JpaRepository<RolePermissionAccess, Integer> {

    RolePermissionAccess findRolePermissionAccessByRolePermissionId(Integer rolePermissionId);

    @Query("SELECT rpa FROM RolePermissionAccess rpa WHERE rpa.rolePermission in (:rolePermissions)")
    List<RolePermissionAccess> getPermissionAccessByRolePermissions(List<RolePermission> rolePermissions);
}
