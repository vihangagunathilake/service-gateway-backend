package com.flex.user_module.impl.repositories;

import com.flex.user_module.api.DTO.RoleNotificationView;
import com.flex.user_module.api.DTO.RolePermissionView;
import com.flex.user_module.impl.entities.Role;
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
public interface RoleRepository extends JpaRepository<Role, Integer> {

    boolean existsByRoleAndDeletedIsFalse(String name);

    boolean existsByIdAndDeletedIsFalse(Integer id);

    Role findByIdAndDeletedIsFalse(Integer id);

    @Query(
            "SELECT " +
                    " r.id AS roleId, " +
                    " r.role AS roleName, " +
                    " p.permission AS permissionName, " +
                    " case when rpa.add_permission is true " +
                    "or rpa.update_permission is true " +
                    "or rpa.delete_permission is true " +
                    "or rpa.get_permission is true " +
                    "or rpa.getAll_permission is true " +
                    "or rpa.assign_permission is true " +
                    "or rpa.all_permission is true then " +
                    "true else false end AS accessSet " +
                    "FROM Role r " +
                    "JOIN RolePermission rp ON rp.role.id = r.id " +
                    "JOIN RolePermissionAccess rpa ON rpa.rolePermission.id = rp.id " +
                    "JOIN Permission p ON rp.permission.id = p.id " +
                    "WHERE r.serviceProvider.id = :serviceProviderId " +
                    "AND r.deleted = false " +
                    "ORDER BY r.id"
    )
    List<RolePermissionView> findRolesWithPermissions(
            @Param("serviceProviderId") Integer serviceProviderId
    );

    @Query(
            "SELECT " +
                    " r.id AS roleId, " +
                    " r.role AS roleName, " +
                    " rn.notificationType.name AS notificationName " +
                    "FROM Role r " +
                    "JOIN RoleNotification rn ON rn.role.id = r.id " +
                    "WHERE r.serviceProvider.id = :serviceProviderId " +
                    "AND r.deleted = false " +
                    "ORDER BY r.id"
    )
    List<RoleNotificationView> findRolesWithNotifications(
            @Param("serviceProviderId") Integer serviceProviderId
    );
}
