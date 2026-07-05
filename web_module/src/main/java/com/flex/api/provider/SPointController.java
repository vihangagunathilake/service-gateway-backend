package com.flex.api.provider;

import com.flex.service_module.api.services.ServicePointService;
import com.flex.service_module.impl.entities.ServicePoint;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/3/2026
 */
@RestController
@RequestMapping("/service-points")
@RequiredArgsConstructor
public class SPointController {

    private final ServicePointService servicePointService;

    @PostMapping("/add")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PM)")
    public ResponseEntity<?> add(@RequestBody ServicePoint servicePoint, HttpServletRequest request) {
        return servicePointService.addServicePoint(servicePoint, request);
    }

    @PostMapping("/update")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PM)")
    public ResponseEntity<?> edit(@RequestBody ServicePoint servicePoint, HttpServletRequest request) {
        return servicePointService.updateServicePoint(servicePoint, request);
    }

    @GetMapping("/service-center/{serviceCenterId}/get-all")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> getAll(@PathVariable Integer serviceCenterId, HttpServletRequest request) {
        return servicePointService.getAllPoints(serviceCenterId, request);
    }

    @DeleteMapping("/{pointId}/delete")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> remove(@PathVariable Integer pointId, HttpServletRequest request) {
        return servicePointService.removePoint(pointId, request);
    }

    @GetMapping("/service-center/{serviceCenterId}/dropdown")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> dropdown(@PathVariable Integer serviceCenterId, HttpServletRequest request) {
        return servicePointService.dropdown(serviceCenterId, request);
    }
}
