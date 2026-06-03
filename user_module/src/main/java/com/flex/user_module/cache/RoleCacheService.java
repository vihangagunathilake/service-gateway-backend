package com.flex.user_module.cache;

import com.flex.user_module.impl.entities.RolePermission;
import com.flex.user_module.impl.repositories.RolePermissionRepository;
import com.flex.user_module.impl.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
@Slf4j
@Service
@SuppressWarnings("Duplicates")
@RequiredArgsConstructor
public class RoleCacheService {

    private final RolePermissionRepository rolePermissionRepository;

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(value = "permissions", key = "#userId")
    public List<RolePermission> cachePermissionsForUser(Integer userId, Integer roleId) {
        return rolePermissionRepository.getAllRolePermissions(roleId);
    }

    @CacheEvict(value = "permissions", key = "#userId")
    public void evictPermissionsCache(Integer userId, String role) {
    }

    //this is for check caching
    public boolean isPermissionsCached(Integer userId) {
        Cache cache = cacheManager.getCache("permissions");
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(userId);
            return valueWrapper != null; // true = cached, false = not cached
        }
        return false; // cache doesn't exist
    }

}
