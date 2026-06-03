package com.flex.api.user;

import com.flex.user_module.api.http.requests.AddRole;
import com.flex.user_module.api.http.requests.RoleAndPermission;
import com.flex.user_module.api.http.requests.RolePermissionAccess;
import com.flex.user_module.api.services.RoleService;
import com.flex.user_module.constants.PermissionConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/15/2026
 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/load-system-permissions")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).RM)")
    public ResponseEntity<?> systemPermissions(HttpServletRequest request) {
        return roleService.systemPermissions(request);
    }

    @PostMapping("/add")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).RM)")
    public ResponseEntity<?> addRole(@RequestBody AddRole addRole, HttpServletRequest request) {
        return roleService.addRole(addRole, request);
    }

    @PostMapping("/update")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).RM)")
    public ResponseEntity<?> updateRole(@RequestBody AddRole addRole, HttpServletRequest request) {
        return roleService.updateRole(addRole, request);
    }

    @PostMapping("/update-permission-access")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PA)")
    public ResponseEntity<?> updateRolePermissionAccess(@RequestBody RolePermissionAccess rolePermissionAccess, HttpServletRequest request) {
        return roleService.updateRolePermissionAccess(rolePermissionAccess, request);
    }

    @PostMapping("/permission-access")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PA)")
    public ResponseEntity<?> getPermissionAccess(@RequestBody RoleAndPermission roleAndPermission,
                                                 HttpServletRequest request) {
        return roleService.getRolePermissionAccess(roleAndPermission, request);
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).RM)")
    public ResponseEntity<?> deleteRole(@PathVariable("id") Integer id, HttpServletRequest request) {
        return roleService.deleteRole(id, request);
    }

    @GetMapping("/get-all")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> getAll(HttpServletRequest request) {
        return roleService.getAllRoles(request);
    }

    @GetMapping("/{id}/get")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).RM)")
    public ResponseEntity<?> getById(@PathVariable Integer id, HttpServletRequest request) {
        return roleService.getRoleById(id, request);
    }
}
