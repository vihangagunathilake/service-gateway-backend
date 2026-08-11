package com.flex.service_module.impl.services;

import com.flex.common_module.http.ReturnResponse;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.service_module.api.services.ServicePointService;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.ServiceCenterRepository;
import com.flex.service_module.impl.repositories.ServicePointRepository;
import com.flex.service_module.impl.repositories.ServiceProviderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.flex.common_module.http.ReturnResponse.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/3/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class SPointServiceImpl implements ServicePointService {

    private final ServicePointRepository servicePointRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceCenterRepository serviceCenterRepository;

    @Override
    public ResponseEntity<?> addServicePoint(ServicePoint servicePoint, HttpServletRequest request) {log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        ServiceProvider provider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (provider == null) {
            return CONFLICT("Service provider not found");
        }

        ServiceCenter serviceCenter = serviceCenterRepository
                .findByIdAndDeletedIsFalse(servicePoint.getServiceCenter().getId());

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        if (!serviceCenter.getOpenTime().equals(servicePoint.getOpenTime())) {
            return CONFLICT("Open time not match with center open time");
        }

        if (!serviceCenter.getCloseTime().equals(servicePoint.getCloseTime())) {
            return CONFLICT("Close time not match with center close time");
        }

        if (servicePointRepository.existsByNameAndServiceCenter_IdAndDeletedIsFalse(servicePoint.getName(),
                serviceCenter.getId())) {
            return CONFLICT("Service point already exists");
        }

        if (servicePoint.getServiceCenter() == null) {
            return CONFLICT("Service center not found");
        }

        servicePoint.setServiceCenter(serviceCenter);

        servicePointRepository.save(servicePoint);

        return SUCCESS("Service point added successfully");
    }

    @Override
    public ResponseEntity<?> updateServicePoint(ServicePoint servicePoint, HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (servicePoint.getId() == null) {
            return BAD_REQUEST("Invalid service point");
        }

        ServicePoint existing = servicePointRepository.findByIdAndDeletedIsFalse(servicePoint.getId());

        if (existing == null) {
            return CONFLICT("Service point not found");
        }

        log.info("id: {}", servicePoint.getId());

        log.info("open: {}", servicePoint.getServiceCenter().getOpenTime());
        log.info("close: {}", servicePoint.getServiceCenter().getCloseTime());

        if (!existing.getServiceCenter().getOpenTime().equals(servicePoint.getOpenTime())) {
            return CONFLICT("Open time not match with center open time");
        }

        if (!existing.getServiceCenter().getCloseTime().equals(servicePoint.getCloseTime())) {
            return CONFLICT("Close time not match with center close time");
        }

        if (servicePoint.getName() != null && !servicePoint.getName().isEmpty() && !servicePoint.getName().equals(existing.getName())) {
            if (servicePointRepository.existsByNameAndIdNotAndDeletedIsFalse(servicePoint.getName(), servicePoint.getId())) {
                return CONFLICT("Service point already exists from this name");
            }

            existing.setName(servicePoint.getName());
        }

        if (servicePoint.getShortName() != null && !servicePoint.getShortName().isEmpty()
                && !servicePoint.getShortName().equals(existing.getShortName())) {
            existing.setShortName(servicePoint.getShortName());
        }

        if (servicePoint.getOpenTime() != null && !servicePoint.getOpenTime().equals(existing.getOpenTime())) {
            existing.setOpenTime(servicePoint.getOpenTime());
        }

        if (servicePoint.getCloseTime() != null && !servicePoint.getCloseTime().equals(existing.getCloseTime())) {
            existing.setCloseTime(servicePoint.getCloseTime());
        }

        if (servicePoint.isTemporaryClosed() != existing.isTemporaryClosed()) {
            existing.setTemporaryClosed(servicePoint.isTemporaryClosed());
        }

        servicePointRepository.save(existing);

        return SUCCESS("Service point updated successfully");
    }

    @Override
    public ResponseEntity<?> getAllPoints(Integer serviceCenterId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (serviceCenterId == null) {
            return BAD_REQUEST("Invalid service center");
        }

        if (!serviceCenterRepository.existsByIdAndDeletedIsFalse(serviceCenterId)) {
            return CONFLICT("Service center not found");
        }

        return DATA(servicePointRepository.servicePointsByCenterDetailed(serviceCenterId));
    }

    @Override
    public ResponseEntity<?> removePoint(Integer pointId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(pointId);

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        //todo: before delete, check pending jobs are available
        // if yes, then noLongerAvailable is true
        // else deleted is true

        servicePoint.setDeleted(true);

        servicePointRepository.save(servicePoint);

        return SUCCESS("Service point removed successfully");
    }

    @Override
    public ResponseEntity<?> dropdown(Integer serviceCenterId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (serviceCenterId == null) {
            return BAD_REQUEST("Invalid service center");
        }

        if (!serviceCenterRepository.existsByIdAndDeletedIsFalse(serviceCenterId)) {
            return CONFLICT("Service center not found");
        }

        return DATA(servicePointRepository.servicePointLightDetailsByCenter(serviceCenterId));
    }
}
