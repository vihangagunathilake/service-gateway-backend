package com.flex.service_module.impl.repositories;

import com.flex.service_module.impl.entities.AvailableService;
import com.flex.service_module.impl.entities.Service;
import com.flex.service_module.impl.entities.ServicePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface AvailableServiceRepository extends JpaRepository<AvailableService,Integer> {

    @Query("SELECT new AvailableService(s.id, s.servicePoint.id, s.service.id) FROM AvailableService s " +
            "WHERE s.service.id=:serviceId AND s.servicePoint.id=:pointId")
    AvailableService availableService(@Param("serviceId") Integer serviceId, @Param("pointId") Integer pointId);

    @Query("SELECT a FROM AvailableService a WHERE a.servicePoint.id=:pointId ORDER BY a.service.orderNumber asc")
    List<AvailableService> findAllByServicePointId(@Param("pointId") Integer pointId);

    @Query("SELECT a.id as id, a.servicePoint.id as pointId, a.service.id as serviceId, a.servicePoint.name as pointName, " +
            "a.servicePoint.serviceCenter.name as serviceCenter FROM AvailableService a WHERE a.service.id=:serviceId")
    List<com.flex.service_module.api.http.DTO.AvailableService> findPointsByServiceId(@Param("serviceId") Integer serviceId);

    @Query("SELECT a.service.id FROM AvailableService a WHERE a.servicePoint.serviceCenter.id=:serviceCenterId group by a.service.id")
    List<Integer> findServicesByServiceCenterId(@Param("serviceCenterId") Integer serviceCenterId);

    @Query("SELECT DISTINCT a.service FROM AvailableService a WHERE a.servicePoint.id in (:ids) ")
    List<Service> servicesInPoints(@Param("ids") List<Integer> pointsIds);

    @Query("SELECT a.service FROM AvailableService a WHERE a.servicePoint.id=:id GROUP BY a.service.id")
    List<Service> servicesInPoint(@Param("id") Integer pointId);

    @Query("SELECT a.servicePoint.id FROM AvailableService a WHERE a.service.id=:serviceId AND a.servicePoint.deleted = false")
    List<Integer> pointsByService(@Param("serviceId") Integer serviceId);

    @Query("""
       SELECT MIN(s.serviceTime)
       FROM AvailableService av
       LEFT JOIN av.service s
       WHERE av.servicePoint.id IN :ids
       """)
    LocalTime findMinimumServiceTimeByServicePointIds(@Param("ids") List<Integer> ids);
}
