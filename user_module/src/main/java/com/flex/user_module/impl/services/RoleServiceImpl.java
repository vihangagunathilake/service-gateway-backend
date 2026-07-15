package com.flex.user_module.impl.services;

import com.flex.common_module.http.ReturnResponse;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.ServiceProviderRepository;
import com.flex.user_module.api.DTO.RoleNotificationView;
import com.flex.user_module.api.DTO.RolePermissionView;
import com.flex.user_module.api.DTO.classes.PermissionDTO;
import com.flex.user_module.api.DTO.classes.RolePermissionDTO;
import com.flex.user_module.api.DTO.classes.RolePermissionsDTO;
import com.flex.user_module.api.http.requests.AddRole;
import com.flex.user_module.api.http.requests.RoleAndPermission;
import com.flex.user_module.api.services.RoleService;
import com.flex.user_module.events.RoleCreatedEvent;
import com.flex.user_module.events.RoleDeleteEvent;
import com.flex.user_module.events.RoleUpdateEvent;
import com.flex.user_module.impl.entities.Permission;
import com.flex.user_module.impl.entities.Role;
import com.flex.user_module.impl.entities.RolePermission;
import com.flex.user_module.impl.entities.RolePermissionAccess;
import com.flex.user_module.impl.repositories.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.flex.common_module.http.ReturnResponse.*;
/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/15/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionAccessRepository rolePermissionAccessRepository;

    private final ApplicationEventPublisher publisher;

    @Override
    public ResponseEntity<?> systemPermissions(HttpServletRequest request) {
        log.info(request.getRequestURI());
        return DATA(permissionRepository.getPermissions());
    }

    @Override
    @Transactional
    public ResponseEntity<?> addRole(AddRole addRole, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} body {}" ,addRole);

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("Not allowed for this action");
        }

        ServiceProvider serviceProvider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (serviceProvider == null) {
            return CONFLICT("You have no access to this service");
        }

        if (roleRepository.existsByRoleAndDeletedIsFalse(addRole.getRoleName())) {
            return CONFLICT("Role already exists");
        }

        if (addRole.getRoleName() == null || addRole.getRoleName().isEmpty()) {
            return BAD_REQUEST("Nothing to add");
        }

        if (addRole.getPermissions() == null || addRole.getPermissions().isEmpty()) {
            return BAD_REQUEST("Nothing to add");
        }

        List<Permission> permissions = permissionRepository.getAlPermissionsByPermission(
                addRole.getPermissions()
        );

        if (permissions == null || permissions.isEmpty()) {
            return CONFLICT("No permissions found");
        }

        Role role = Role.builder()
                .role(addRole.getRoleName())
                .serviceProvider(serviceProvider)
                .build();

        roleRepository.save(role);
        roleRepository.flush();

        List<RolePermission> rolePermissions = permissions.stream().map(
                permission -> RolePermission.builder().role(role).permission(permission).build()
        ).toList();

        List<RolePermissionAccess> rolePermissionAccesses = rolePermissions.stream().map(
                rolePermission ->
                        RolePermissionAccess.builder()
                                .rolePermission(rolePermission)
                                .build()
        ).toList();

        // adding role notification is happening in notification_module
        // this is added to prevent circular dependency
        publisher.publishEvent(
                new RoleCreatedEvent(
                        role.getId(),
                        addRole.getNotifications()
                )
        );

        rolePermissionRepository.saveAll(rolePermissions);
        rolePermissionRepository.flush();

        rolePermissionAccessRepository.saveAll(rolePermissionAccesses);
        rolePermissionAccessRepository.flush();

        return SUCCESS("Role added");
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateRole(AddRole addRole, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} body {}" ,addRole);

        if (addRole.getRoleId() == null) {
            return BAD_REQUEST("Role id is not found");
        }

        if (addRole.getPermissions() == null || addRole.getPermissions().isEmpty()) {
            return BAD_REQUEST("Permissions can not be empty");
        }

        Role role = roleRepository.findByIdAndDeletedIsFalse(addRole.getRoleId());

        if (role == null) {
            return CONFLICT("Role not found");
        }

        boolean roleUpdated = false;
        boolean permissionsUpdated = false;

        if (addRole.getRoleName() != null
                && !addRole.getRoleName().isEmpty()
                && !role.getRole().equals(addRole.getRoleName())) {
            role.setRole(addRole.getRoleName());

            roleRepository.save(role);

            roleUpdated = true;
        }

        List<String> rolePermissions = rolePermissionRepository.getRolePermissions(addRole.getRoleId());

        List<String> deletingPermission = rolePermissions.stream().filter(
                rolePermission -> !addRole.getPermissions().contains(rolePermission)
        ).toList();

        List<String> addingPermission = addRole.getPermissions().stream().filter(
                rolePermission -> !rolePermissions.contains(rolePermission)
        ).toList();

        if (!deletingPermission.isEmpty()) {
            List<RolePermission> deletingRolePermissions = rolePermissionRepository
                    .getRolePermissionsByPermissions(addRole.getRoleId(), deletingPermission);

            List<RolePermissionAccess> deletingRolePermissionAccess = rolePermissionAccessRepository
                    .getPermissionAccessByRolePermissions(deletingRolePermissions);

            rolePermissionAccessRepository.deleteAll(deletingRolePermissionAccess);

            rolePermissionRepository.deleteAll(deletingRolePermissions);
            permissionsUpdated = true;
        }

        if (!addingPermission.isEmpty()) {

            List<Permission> addingPermissions = permissionRepository.getAlPermissionsByPermission(addingPermission);

            List<RolePermission> newRolePermissions = addingPermissions.stream().map(
                    permission -> RolePermission.builder()
                            .role(role)
                            .permission(permission)
                            .build()
            ).toList();

            List<RolePermissionAccess> newRolePermissionAccess = newRolePermissions.stream().map(
                    rolePermission -> RolePermissionAccess.builder()
                            .rolePermission(rolePermission)
                            .build()
            ).toList();

            rolePermissionRepository.saveAll(newRolePermissions);
            rolePermissionAccessRepository.saveAll(newRolePermissionAccess);
            permissionsUpdated = true;
        }

        // updating role notification is happening in notification_module
        // this is added to prevent circular dependency
        publisher.publishEvent(
                new RoleUpdateEvent(
                        role.getId(),
                        addRole.getNotifications()
                )
        );

        if (roleUpdated && permissionsUpdated) {
            return SUCCESS("Role and permissions updated");
        }

        if (roleUpdated) {
            return SUCCESS("Role updated");
        }

        if (permissionsUpdated) {
            return SUCCESS("Permissions updated");
        }

        return SUCCESS("Nothing to update");

    }

    @Override
    public ResponseEntity<?> updateRolePermissionAccess(com.flex.user_module.api.http.requests.RolePermissionAccess rolePermissionAccess,
                                                        HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (!rolePermissionRepository.existsById(rolePermissionAccess.getRolePermissionId())) {
            log.error("{} not found", rolePermissionAccess.getRolePermissionId());
            return CONFLICT("Role permission not found");
        }

        RolePermissionAccess existingRolePermissionAccess = rolePermissionAccessRepository
                .findRolePermissionAccessByRolePermissionId(rolePermissionAccess.getRolePermissionId());

        if (existingRolePermissionAccess == null) {
            return CONFLICT("Role permission access not found");
        }

        log.info("si??????? {} ", rolePermissionAccess.isAssign());

        existingRolePermissionAccess.setAdd_permission(rolePermissionAccess.isAdd());
        existingRolePermissionAccess.setUpdate_permission(rolePermissionAccess.isUpdate());
        existingRolePermissionAccess.setDelete_permission(rolePermissionAccess.isDelete());
        existingRolePermissionAccess.setGet_permission(rolePermissionAccess.isGet());
        existingRolePermissionAccess.setGetAll_permission(rolePermissionAccess.isGetAll());
        existingRolePermissionAccess.setAssign_permission(rolePermissionAccess.isAssign());
        existingRolePermissionAccess.setAll_permission(rolePermissionAccess.isAll());

        rolePermissionAccessRepository.save(existingRolePermissionAccess);

        return SUCCESS("Permission access updated");
    }

    @Override
    public ResponseEntity<?> getRolePermissionAccess(RoleAndPermission roleAndPermission, HttpServletRequest request) {
        log.info(request.getRequestURI());

        RolePermission rolePermission = rolePermissionRepository
                .findByRoleIdAndPermission_Permission(roleAndPermission.getRoleId(), roleAndPermission.getPermission());

        if (rolePermission == null) {
            return CONFLICT("Role permission not found");
        }

        RolePermissionAccess existingRolePermissionAccess = rolePermissionAccessRepository
                .findRolePermissionAccessByRolePermissionId(rolePermission.getId());

        if (existingRolePermissionAccess == null) {
            existingRolePermissionAccess = RolePermissionAccess.builder()
                    .rolePermission(rolePermission)
                    .build();

            existingRolePermissionAccess = rolePermissionAccessRepository.save(existingRolePermissionAccess);
        }

        return DATA(existingRolePermissionAccess);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteRole(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} id {}" ,id);

        if (id == null) {
            return BAD_REQUEST("Role id is not found");
        }

        Role role = roleRepository.findByIdAndDeletedIsFalse(id);

        if (role == null) {
            return CONFLICT("Role not found");
        }

        role.setDeleted(true);

        roleRepository.save(role);

        List<RolePermission> rolePermissions = rolePermissionRepository
                .getAllRolePermissions(id);

        rolePermissionRepository.deleteAll(rolePermissions);

        // deleting role notification is happening in notification_module
        // this is added to prevent circular dependency
        publisher.publishEvent(
                new RoleDeleteEvent(
                        role.getId()
                )
        );

        return SUCCESS("Role deleted");
    }

    @Override
    public ResponseEntity<?> getAllRoles(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("Not allowed for this action");
        }

        ServiceProvider serviceProvider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (serviceProvider == null) {
            return CONFLICT("You have no access to this service");
        }

        List<RolePermissionView> rolePermissionViews = roleRepository.findRolesWithPermissions(serviceProvider.getId());

        List<RoleNotificationView> roleNotificationViews =
                roleRepository.findRolesWithNotifications(serviceProvider.getId());

        Map<Integer, RolePermissionsDTO> roleMap = new LinkedHashMap<>();

        // permissions
        for (RolePermissionView row : rolePermissionViews) {

            RolePermissionsDTO dto = roleMap.computeIfAbsent(
                    row.getRoleId(),
                    id -> new RolePermissionsDTO(
                            id,
                            row.getRoleName(),
                            new ArrayList<>(),
                            new ArrayList<>()
                    )
            );

            if (row.getPermissionName() != null &&
                    dto.getPermissions().stream()
                            .noneMatch(p -> p.getName().equals(row.getPermissionName()))) {

                dto.getPermissions().add(
                        PermissionDTO.builder()
                                .name(row.getPermissionName())
                                .allowed(Boolean.TRUE.equals(row.getAccessSet()))
                                .build()
                );
            }
        }

        // notifications
        for (RoleNotificationView row : roleNotificationViews) {

            RolePermissionsDTO dto = roleMap.computeIfAbsent(
                    row.getRoleId(),
                    id -> new RolePermissionsDTO(
                            id,
                            row.getRoleName(),
                            new ArrayList<>(),
                            new ArrayList<>()
                    )
            );

            if (row.getNotificationName() != null &&
                    !dto.getNotifications().contains(row.getNotificationName())) {
                dto.getNotifications().add(row.getNotificationName());
            }
        }

        return DATA(new ArrayList<>(roleMap.values()));
    }

    @Override
    public ResponseEntity<?> getRoleById(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} id {}" ,id);

        Role role = roleRepository.findByIdAndDeletedIsFalse(id);

        if (role == null) {
            return CONFLICT("Role not found");
        }

        List<String> rolePermissions = rolePermissionRepository.getRolePermissions(id);

        RolePermissionDTO rolePermissionsDTO = RolePermissionDTO.builder()
                .id(role.getId())
                .name(role.getRole())
                .permissions(rolePermissions)
                .build();

        return DATA(rolePermissionsDTO);
    }
}
