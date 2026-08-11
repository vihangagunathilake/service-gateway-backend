package com.flex.service_module.impl.repositories;

import com.flex.service_module.impl.entities.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Integer> {

    boolean existsByNameAndEmailAndDeletedIsFalse(String name, String email);

    boolean existsByIdAndDeletedIsFalse(Integer id);

    ServiceProvider findByProviderIdAndDeletedIsFalse(String providerId);

    ServiceProvider findByIdAndDeletedIsFalse(Integer id);
}
