package com.flex.user_module.impl.repositories;

import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.user_module.api.DTO.CenterUsers;
import com.flex.user_module.api.DTO.UserDropdown;
import com.flex.user_module.api.DTO.UserPermissionAccessView;
import com.flex.user_module.api.DTO.UsersList;
import com.flex.user_module.impl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmailAndDeletedIsFalse(String email);

    boolean existsByIdAndDeletedIsFalse(Integer id);

    User findByEmailAndDeletedIsFalse(String email);

    User findByIdAndDeletedIsFalse(Integer id);

    @Query("SELECT count(u) FROM User u WHERE u.serviceProvider.id=:spId " +
            "AND u.serviceCenter.deleted = false AND u.userType=:userType AND u.deleted = false")
    int getCountByServiceProviderAndDeletedIsFalse(@Param("spId")Integer serviceProviderId,
                                                   @Param("userType") Integer userType);

    @Query(
            "SELECT " +
                    " u.id AS id, " +
                    " u.fName AS firstName, " +
                    " u.lName AS lastName, " +
                    " u.email AS email, " +
                    " ud.contact AS mobile, " +
                    " ud.nic AS nic, " +
                    " case when u.userType = 0 then 'admin' " +
                    "   when u.userType = 1 then 'user' " +
                    "   when u.userType = 2 then 'employee' " +
                    "   else 'unknown' end AS userType, " +
                    " r.role AS role, " +
                    " sc.name AS serviceCenter, " +
                    " case when us.providerApproved = true then 'approved' else 'pending' end AS providerApproved, " +
                    " u.profileImageUrl AS profileImage " +
                    "FROM User u " +
                    " LEFT JOIN UserDetails ud ON ud.user = u " +
                    " LEFT JOIN UserStatus us ON us.user = u " +
                    " LEFT JOIN u.role r " +
                    " LEFT JOIN u.serviceCenter sc " +
                    "WHERE u.deleted = false AND u.id <> :userId " +
                    " AND u.userType <> 1 " +
                    " AND u.serviceProvider.id = :serviceProviderId " +
                    " AND ( " +
                    "   LOWER(u.fName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "   LOWER(u.lName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "   ud.contact LIKE CONCAT('%', :search, '%') OR " +
                    "   ud.nic LIKE CONCAT('%', :search, '%') " +
                    " )"
    )
    Page<UsersList> findAllByServiceProvider(
            @Param("serviceProviderId") Integer serviceProviderId,
            @Param("search") String search,
            @Param("userId") Integer userId,
            Pageable pageable
    );

    @Query(
            value =
                    "SELECT " +
                            " u.id AS userId, " +
                            " CONCAT(u.f_name, ' ', u.l_name) AS userName, " +
                            " ud.contact AS contact, " +
                            " u.email AS email, " +
                            " r.role AS role " +
                            "FROM users u " +
                            "LEFT JOIN user_details ud ON u.id = ud.user_id " +
                            "LEFT JOIN roles r ON u.role_id = r.id " +
                            "WHERE u.service_center_id = :id " +
                            "AND u.deleted = false",
            nativeQuery = true
    )
    List<CenterUsers> findUsersByServiceCenter(
            @Param("id") Integer id
    );

    @Query("SELECT u.id as id, CONCAT(u.fName, ' ', u.lName) AS name FROM User u " +
            "WHERE u.serviceProvider.id=:providerId AND (u.serviceCenter is null OR u.serviceCenter.id <> :centerId) " +
            "AND u.userType <> 1 AND u.deleted is false")
    List<UserDropdown> getNonAssignedUsers(@Param("providerId") Integer serviceProviderId,
                                           @Param("centerId") Integer serviceCenterId);

    @Query("""
        SELECT
            p.permission AS permission,
    
            rpa.add_permission AS adding,
            rpa.update_permission AS updating,
            rpa.delete_permission AS deleting,
            rpa.get_permission AS getting,
            rpa.getAll_permission AS getAll,
            rpa.assign_permission AS assigning,
            rpa.all_permission AS allowAll
    
        FROM User u
    
        LEFT JOIN Role r
            ON u.role.id = r.id
    
        LEFT JOIN RolePermission rp
            ON r.id = rp.role.id
    
        LEFT JOIN Permission p
            ON rp.permission.id = p.id
    
        LEFT JOIN RolePermissionAccess rpa
            ON rpa.rolePermission.id = rp.id
    
        WHERE r.id IS NOT NULL
        AND u.id = :userId
    """)
    List<UserPermissionAccessView> getUserPermissionAccess(
            @Param("userId") Integer userId
    );

    @Query("SELECT u.id FROM User u WHERE u.serviceProvider.id=:id AND u.deleted = false")
    List<Integer> getUserIdsByServiceProvider(@Param("id") Integer id);
}
