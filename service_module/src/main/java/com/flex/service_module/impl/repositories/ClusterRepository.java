package com.flex.service_module.impl.repositories;

import com.flex.service_module.impl.entities.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClusterRepository extends JpaRepository<Cluster, Integer> {

    boolean existsByNameAndDeletedIsFalse(String name);

    boolean existsByNameAndIdNotAndDeletedIsFalse(String name, Integer id);

    Cluster findByIdAndDeletedIsFalse(Integer id);

    List<Cluster> findAllByServiceProvider_IdAndDeletedIsFalse(Integer providerId);

    @Query("SELECT c.id FROM Cluster c WHERE c.name = :name AND c.deleted = false")
    Integer getClusterIdByName(@Param("name") String name);
}
