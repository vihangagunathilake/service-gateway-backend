package com.flex.service_module.impl.services;

import com.flex.common_module.http.pagination.Pagination;
import com.flex.common_module.http.pagination.Sorting;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.service_module.api.http.DTO.CenterClusterData;
import com.flex.service_module.api.http.DTO.classes.ServiceCenterViewDTO;
import com.flex.service_module.api.http.responses.LoadCenterServices;
import com.flex.service_module.api.services.SCService;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.*;
import com.flex.service_module.impl.services.helpers.SCServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.flex.common_module.http.ReturnResponse.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/1/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class SCServiceImpl implements SCService {

    private final SCServiceHelper sCServiceHelper;

    private final ServiceCenterRepository serviceCenterRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    private final CenterClusterRepository centerClusterRepository;
    private final ServicePointRepository servicePointRepository;
    private final AvailableServiceRepository availableServiceRepository;

    @Override
    public ResponseEntity<?> addNewCenter(ServiceCenter serviceCenter, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        ServiceProvider provider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (provider == null) {
            return CONFLICT("Service provider not found");
        }

        if (serviceCenterRepository.existsByNameAndServiceProvider_IdAndDeletedIsFalse(
                serviceCenter.getName(), provider.getId()
        )) {
            return CONFLICT("Service center already created");
        }

        String status = sCServiceHelper.validateServiceCenter(serviceCenter);

        if (!status.equals("fine")) {
            return CONFLICT(status);
        }

        serviceCenter.setAddedTime(new Date());
        serviceCenter.setServiceProvider(provider);

        serviceCenterRepository.save(serviceCenter);

        return SUCCESS("Service center created");
    }

    @Override
    public ResponseEntity<?> updateCenter(ServiceCenter serviceCenter, HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (serviceCenter.getId() == null) {
            return CONFLICT("Can not update this record");
        }

        ServiceCenter existing = serviceCenterRepository.findByIdAndDeletedIsFalse(
                serviceCenter.getId()
        );

        if (existing == null) {
            return CONFLICT("Service center not found");
        }

        if (serviceCenter.getName() != null && !serviceCenter.getName().isEmpty()
                && !existing.getName().equals(serviceCenter.getName())) {
            if (serviceCenterRepository.existsByNameAndIdNotAndDeletedIsFalse(serviceCenter.getName(), existing.getId())) {
                return CONFLICT("Service center already exists by this name");
            }
            existing.setName(serviceCenter.getName());
        }

        if (serviceCenter.getContact() != null && !serviceCenter.getContact().isEmpty()
                && !existing.getContact().equals(serviceCenter.getContact())) {
            if (serviceCenterRepository.existsByContactAndIdNotAndDeletedIsFalse(serviceCenter.getContact(), existing.getId())) {
                return CONFLICT("This contact already exists");
            }
            existing.setContact(serviceCenter.getContact());
        }

        if (serviceCenter.getLocation() != null
                && !serviceCenter.getLocation().isEmpty()
                && !existing.getLocation().equals(serviceCenter.getLocation())) {
            existing.setLocation(serviceCenter.getLocation());
        }

        if (serviceCenter.getOpenTime() != null
                && !existing.getOpenTime().equals(serviceCenter.getOpenTime())) {
            existing.setOpenTime(serviceCenter.getOpenTime());
        }

        if (serviceCenter.getCloseTime() != null
                && !existing.getCloseTime().equals(serviceCenter.getCloseTime())) {
            existing.setCloseTime(serviceCenter.getCloseTime());
        }

        serviceCenterRepository.save(existing);

        return SUCCESS("Service center updated");
    }

    @Override
    public ResponseEntity<?> getAllCenters(Pagination pagination, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        Sort sort = Sort.by(Sorting.getSort(pagination.getSort()));

        Pageable pageable = PageRequest.of(
                pagination.getPage(),
                pagination.getSize(),
                sort
        );

        Page<ServiceCenter> result;

        //this is ok
        Integer serviceCenterId = serviceCenterRepository.findByServiceUserId(
                userClaims.getUserId()
        );

        if (serviceCenterId == null) {
            ServiceProvider provider = serviceProviderRepository
                    .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

            if (provider == null) {
                return CONFLICT("Service provider not found");
            }

            if (pagination.getSearchText() == null || pagination.getSearchText().trim().isEmpty()) {
                result = serviceCenterRepository
                        .findByServiceProvider_IdAndDeletedFalse(provider.getId(), pageable);
            } else {
                result = serviceCenterRepository
                        .searchCenters(provider.getId(), pagination.getSearchText().trim(), pageable);
            }
        } else {
            result = serviceCenterRepository
                    .findByIdAndDeletedFalse(serviceCenterId, pageable);
        }

        return DATA(
                result.map(sc -> ServiceCenterViewDTO.builder()
                        .id(sc.getId())
                        .name(sc.getName())
                        .location(sc.getLocation())
                        .contact(sc.getContact())
                        .openTime(sc.getOpenTime())
                        .closeTime(sc.getCloseTime())
                        .fOpenTime(sCServiceHelper.formatTimeRange(sc.getOpenTime()))
                        .fCloseTime(sCServiceHelper.formatTimeRange(sc.getCloseTime()))
                        .build())
        );
    }

    @Override
    public ResponseEntity<?> getAllCenters(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        ServiceProvider provider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (provider == null) {
            return CONFLICT("Service provider not found");
        }

        return DATA(serviceCenterRepository.findCentersWithStatus(provider.getId(), LocalTime.now()));
    }

    @Override
    public ResponseEntity<?> getCenter(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(id);

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        return DATA(ServiceCenterViewDTO.builder()
                .id(serviceCenter.getId())
                .name(serviceCenter.getName())
                .location(serviceCenter.getLocation())
                .contact(serviceCenter.getContact())
                .fOpenTime(sCServiceHelper.formatTimeRange(serviceCenter.getOpenTime()))
                .fCloseTime(sCServiceHelper.formatTimeRange(serviceCenter.getCloseTime()))
                .build());
    }

    @Override
    public ResponseEntity<?> getAllForDropdown(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        //this is ok
        Integer serviceCenterId = serviceCenterRepository.findByServiceUserId(
                userClaims.getUserId()
        );

        if (serviceCenterId == null) {
            ServiceProvider provider = serviceProviderRepository
                    .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

            if (provider == null) {
                return CONFLICT("Service provider not found");
            }

            return DATA(serviceCenterRepository.findCentersForDropdown(provider.getId()));
        } else {
            return DATA(serviceCenterRepository.singleBranchDropdown(serviceCenterId));
        }


    }

    @Override
    public ResponseEntity<?> deleteCenter(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(id);

        if (serviceCenter == null) {
            return CONFLICT("Service center not found");
        }

        serviceCenter.setDeleted(true);

        serviceCenterRepository.save(serviceCenter);

        return SUCCESS("Service center deleted");
    }

    @Override
    public ResponseEntity<?> commonCenterServices(Integer centerId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (!serviceCenterRepository.existsByIdAndDeletedIsFalse(centerId)) {
            return CONFLICT("Service center not found");
        }

        List<LoadCenterServices> loadCenterServices = new ArrayList<>();

        List<CenterClusterData> centerClusters = centerClusterRepository.getClustersByCenterId(centerId);

        // add center cluster
        if (!centerClusters.isEmpty()) {
            for (CenterClusterData centerClusterData : centerClusters) {
                LoadCenterServices centerService = LoadCenterServices.builder()
                        .serviceId(centerClusterData.getId())
                        .service(centerClusterData.getName())
                        .cluster(true)
                        .build();

                loadCenterServices.add(centerService);
            }
        }

        // add available services individually
        List<Integer> servicePoints = servicePointRepository.servicePointIdsByCenter(centerId);

        List<com.flex.service_module.impl.entities.Service> individualServicesInCenter = availableServiceRepository
                .servicesInPoints(servicePoints);

        if (!individualServicesInCenter.isEmpty()) {
            for (com.flex.service_module.impl.entities.Service service : individualServicesInCenter) {
                LoadCenterServices centerService = LoadCenterServices.builder()
                        .serviceId(service.getId())
                        .service(service.getName())
                        .cluster(false)
                        .build();

                loadCenterServices.add(centerService);
            }
        }

        return DATA(loadCenterServices);
    }
}
