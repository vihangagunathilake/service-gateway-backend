package com.flex.api.provider;

import com.flex.service_module.api.services.SPService;
import com.flex.service_module.impl.entities.ServiceProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/30/2026
 */
@RestController
@RequestMapping("/service-provider")
@RequiredArgsConstructor
public class SPController {

    private final SPService SPService;

    @GetMapping("/profile")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).PT)")
    public ResponseEntity<?> profile(HttpServletRequest request) {
        return SPService.serviceProviderProfile(request);
    }

    @PostMapping("/edit")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).SP)")
    public ResponseEntity<?> editSP(@RequestBody ServiceProvider serviceProvider , HttpServletRequest request) {
        return SPService.editServiceProvider(serviceProvider, request);
    }

    @PostMapping("/profile-image")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).SP)")
    public ResponseEntity<?> uploadSPProfileImage(@RequestParam(required = false) MultipartFile multipartFile , HttpServletRequest request) {
        return SPService.uploadSPProfileImage(multipartFile, request);
    }

    @PostMapping("/cover-image")
    @PreAuthorize("@securityService.hasAnyAccess(T(com.flex.user_module.constants.PermissionConstant).SP)")
    public ResponseEntity<?> uploadSPCoverImage(@RequestParam(required = false) MultipartFile multipartFile , HttpServletRequest request) {
        return SPService.uploadSPCoverPhoto(multipartFile, request);
    }
}
