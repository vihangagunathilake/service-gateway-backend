package com.flex.service_module.api.services;

import com.flex.service_module.impl.entities.ServicePoint;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/3/2026
 */
public interface ServicePointService {

    ResponseEntity<?> addServicePoint(ServicePoint servicePoint, HttpServletRequest request);

    ResponseEntity<?> updateServicePoint(ServicePoint servicePoint, HttpServletRequest request);

    ResponseEntity<?> getAllPoints(Integer serviceCenterId, HttpServletRequest request);

    ResponseEntity<?> removePoint(Integer pointId, HttpServletRequest request);

    ResponseEntity<?> dropdown(Integer serviceCenterId, HttpServletRequest request);
}
