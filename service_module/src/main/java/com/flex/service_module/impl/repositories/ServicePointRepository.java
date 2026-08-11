package com.flex.service_module.impl.repositories;

import com.flex.service_module.api.http.DTO.BestServicePointForJob;
import com.flex.service_module.impl.entities.ServicePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ServicePointRepository extends JpaRepository<ServicePoint, Integer> {
    boolean existsByNameAndServiceCenter_IdAndDeletedIsFalse(String name, Integer serviceCenterId);

    boolean existsByNameAndIdNotAndDeletedIsFalse(String name, Integer id);

    ServicePoint findByIdAndDeletedIsFalse(Integer id);

    List<ServicePoint> findAllByServiceCenter_IdAndDeletedIsFalse(Integer serviceCenterId);

    @Query("SELECT count(sp) FROM ServicePoint sp WHERE sp.serviceCenter.serviceProvider.id=:spId " +
            "AND sp.serviceCenter.deleted = false AND sp.deleted = false")
    int getCountByServiceProviderId(@Param("spId") Integer serviceCenterId);

    @Query("SELECT new ServicePoint(s.id, s.name, s.shortName, s.openTime, s.closeTime, s.temporaryClosed, s.deleted, " +
            "s.serviceCenter.id, s.serviceCenter.name, COUNT(av.service.id)) " +
            "FROM ServicePoint s " +
            "LEFT JOIN AvailableService av ON av.servicePoint.id = s.id " +
            "WHERE s.serviceCenter.id=:serviceCenterId and s.deleted is false " +
            "GROUP BY " +
            "        s.id, s.name, s.shortName, " +
            "        s.openTime, s.closeTime, " +
            "        s.temporaryClosed, s.deleted, " +
            "        s.id, s.name")
    List<ServicePoint> servicePointsByCenter(@Param("serviceCenterId") Integer serviceCenterId);

    @Query("SELECT new ServicePoint(s.id, s.name, s.shortName, s.openTime, s.closeTime, s.temporaryClosed, s.deleted, " +
            "s.serviceCenter.id, s.serviceCenter.name, COUNT(av.service.id), s.noLongerAvailable) " +
            "FROM ServicePoint s " +
            "LEFT JOIN AvailableService av ON av.servicePoint.id = s.id " +
            "WHERE s.serviceCenter.id=:serviceCenterId and s.deleted is false " +
            "GROUP BY " +
            "        s.id, s.name, s.shortName, " +
            "        s.openTime, s.closeTime, " +
            "        s.temporaryClosed, s.deleted, " +
            "        s.id, s.name")
    List<ServicePoint> servicePointsByCenterDetailed(@Param("serviceCenterId") Integer serviceCenterId);

    @Query("SELECT new ServicePoint(s.id, s.name) " +
            "FROM ServicePoint s " +
            "WHERE s.serviceCenter.id=:serviceCenterId and s.deleted is false")
    List<ServicePoint> servicePointLightDetailsByCenter(@Param("serviceCenterId") Integer serviceCenterId);

    @Query("SELECT s.id FROM ServicePoint s WHERE s.serviceCenter.id=:serviceCenterId and s.deleted is false")
    List<Integer> servicePointIdsByCenter(@Param("serviceCenterId") Integer serviceCenterId);

    @Query(value = """
            SELECT sp.id AS id,
                   sp.name AS name,
                   SEC_TO_TIME(SUM(TIME_TO_SEC(jp.end_time) - TIME_TO_SEC(jp.start_time))) AS totalJobTime
            FROM service_point sp
                     LEFT JOIN available_services av 
                            ON av.service_point_id = sp.id
                     LEFT JOIN jobs_at_point jp 
                            ON jp.service_point_id = sp.id
            WHERE sp.service_center_id = :serviceCenterId
              AND av.service_id = :serviceId
              AND sp.deleted = false
            GROUP BY sp.id
            ORDER BY 
                (SUM(TIME_TO_SEC(jp.end_time) - TIME_TO_SEC(jp.start_time)) IS NOT NULL),
                SUM(TIME_TO_SEC(jp.end_time) - TIME_TO_SEC(jp.start_time)) ASC
            LIMIT 1
            """,
            nativeQuery = true)
    BestServicePointForJob findBestServicePoint(
            @Param("serviceCenterId") Integer serviceCenterId,
            @Param("serviceId") Integer serviceId);

    @Query("""
        SELECT sp
        FROM ServicePoint sp
        JOIN AvailableService av ON av.servicePoint.id = sp.id
        WHERE sp.serviceCenter.id = :centerId AND av.service.id IN :serviceIds
          AND sp.deleted = false
          AND sp.temporaryClosed = false
        GROUP BY sp.id
        HAVING COUNT(DISTINCT av.service.id) = :size
    """)
    List<ServicePoint> findServicePointsHavingAllServices(
            @Param("serviceIds") List<Integer> serviceIds,
            @Param("centerId") Integer centerId,
            @Param("size") long size
    );

    @Query("""
        SELECT al.id
        FROM AgentLogin al
        WHERE al.servicePoint.id = :servicePointId
          AND al.loginDate = current_date()
          AND al.logoutTime IS NULL
    """)
    Integer loginAgentAtPoint(@Param("servicePointId") Integer servicePointId);
}
