package com.flex.common_module.security.services;

import com.flex.common_module.constants.Colors;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
@Slf4j
@Component("securityService")
public class SecurityService {
    public boolean hasAnyAccess(String... permissions) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn(Colors.YELLOW + "Authentication: " + auth + Colors.RESET);
            if (auth != null) log.warn(Colors.YELLOW + "Authentication Status: "
                    + auth.isAuthenticated() + Colors.RESET);

            return false;
        }

        // 🔐 Else check if user has any of the specified permissions
        Set<String> grantedAuthorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String required : permissions) {
            if (grantedAuthorities.contains(required)) {
                return true;
            }
        }

        //this part for debug
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            log.warn("Access deny for {}", request.getRequestURI());
        }

        log.error(Colors.YELLOW + "No {} found" + Colors.RESET, Arrays.toString(permissions));
        return false;
    }
}
