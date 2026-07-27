package com.flex.service_module.impl.repositories;

import com.flex.service_module.api.http.DTO.ServiceCenterStatusView;
import com.flex.service_module.api.http.DTO.ServiceCentersDropdown;
import com.flex.service_module.api.http.DTO.classes.ServiceCenterViewDTO;
import com.flex.service_module.impl.entities.ServiceCenter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
public interface ServiceCenterRepository extends JpaRepository<ServiceCenter, Integer> {

    boolean existsByIdAndDeletedIsFalse(Integer id);

    boolean existsByNameAndServiceProvider_IdAndDeletedIsFalse(String name, Integer SPId);

    boolean existsByNameAndIdNotAndDeletedIsFalse(String name, Integer id);

    boolean existsByContactAndIdNotAndDeletedIsFalse(String contact, Integer id);

    ServiceCenter findByIdAndDeletedIsFalse(Integer id);

    Page<ServiceCenter> findByServiceProvider_IdAndDeletedFalse(
            Integer providerId,
            Pageable pageable
    );

    Page<ServiceCenter> findByIdAndDeletedFalse(
            Integer id,
            Pageable pageable
    );

    @Query("SELECT count(sc) FROM ServiceCenter sc WHERE sc.serviceProvider.id=:providerId AND sc.deleted = false")
    int getCountByServiceProvider(@Param("providerId") Integer providerId);

    @Query(
            "SELECT sc FROM ServiceCenter sc " +
                    "WHERE sc.serviceProvider.id = :providerId " +
                    "AND sc.deleted = false " +
                    "AND (LOWER(sc.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "OR LOWER(sc.contact) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<ServiceCenter> searchCenters(
            @Param("providerId") Integer providerId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(
            value = "SELECT c.id AS id, " +
                    "c.name AS name, " +
                    "c.contact AS contact, " +
                    "c.location AS location, " +
                    "CASE WHEN :time BETWEEN c.open_time AND c.close_time " +
                    "THEN 'Opened' ELSE 'Closed' END AS status " +
                    "FROM service_centers c " +
                    "WHERE c.service_provider_id = :providerId " +
                    "AND c.deleted = false",
            nativeQuery = true
    )
    List<ServiceCenterStatusView> findCentersWithStatus(
            @Param("providerId") Integer providerId,
            @Param("time") LocalTime time
    );

    @Query(
            value = "SELECT c.id AS id, " +
                    "c.name AS name, " +
                    "c.location AS location " +
                    "FROM service_centers c " +
                    "WHERE c.service_provider_id = :providerId " +
                    "AND c.deleted = false",
            nativeQuery = true
    )
    List<ServiceCentersDropdown> findCentersForDropdown(
            @Param("providerId") Integer providerId
    );

    @Query(
            value = "SELECT c.id AS id, " +
                    "c.name AS name, " +
                    "c.location AS location " +
                    "FROM service_centers c " +
                    "WHERE c.id = :id " +
                    "AND c.deleted = false",
            nativeQuery = true
    )
    List<ServiceCentersDropdown> singleBranchDropdown(
            @Param("id") Integer id
    );

    //this is ok. this is done because prevent circular dependency issue
    @Query("""
        SELECT s.id
        FROM ServiceCenter s
        JOIN User u ON u.serviceCenter.id = s.id
        WHERE u.id = :userId
        AND s.deleted = false
        AND u.deleted = false
    """)
    Integer findByServiceUserId(@Param("userId") Integer userId);
}

