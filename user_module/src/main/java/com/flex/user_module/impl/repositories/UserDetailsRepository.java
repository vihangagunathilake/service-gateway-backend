package com.flex.user_module.impl.repositories;

import com.flex.user_module.impl.entities.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/25/2026
 */
public interface UserDetailsRepository extends JpaRepository<UserDetails, Integer> {

    UserDetails findByNic(String nic);

    boolean existsByNic(String nic);

    boolean existsByContact(String contact);

    UserDetails findByUser_id(Integer userId);


}
