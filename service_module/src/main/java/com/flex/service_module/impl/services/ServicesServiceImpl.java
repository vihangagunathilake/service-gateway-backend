package com.flex.service_module.impl.services;

import com.flex.common_module.http.pagination.Pagination;
import com.flex.common_module.http.pagination.Sorting;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.service_module.api.http.DTO.classes.ServiceViewDTO;
import com.flex.service_module.api.http.requests.AssignServiceToPoint;
import com.flex.service_module.api.services.ServicesService;
import com.flex.service_module.impl.entities.AvailableService;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.*;
import com.flex.service_module.impl.services.helpers.ServicesServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.flex.common_module.http.ReturnResponse.*;;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 2/2/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class ServicesServiceImpl implements ServicesService {

    private final ServicesServiceHelper servicesServiceHelper;

    private final ServicesRepository servicesRepository;
    private final ServicePointRepository servicePointRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final AvailableServiceRepository availableServiceRepository;
    private final ServiceCenterRepository serviceCenterRepository;

    @Override
    public ResponseEntity<?> addService(com.flex.service_module.impl.entities.Service service,
            HttpServletRequest request) {
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

        List<com.flex.service_module.impl.entities.Service> serviceList = servicesRepository
                .findAllByProvider_IdAndDeletedIsFalseOrderByOrderNumberAsc(provider.getId());

        int orderNumber = 1;

        if (!serviceList.isEmpty()) {
            orderNumber = serviceList.getLast().getOrderNumber() + 1;
        }

        // 🔒 Check duplicate service name
        if (servicesRepository.existsByNameAndProvider_IdAndDeletedIsFalse(
                service.getName(), provider.getId())) {
            return CONFLICT("Service already exists");
        }

        // 🧹 Optional validations
        if (service.getName() == null || service.getName().trim().isEmpty()) {
            return CONFLICT("Service name is required");
        }

        if (service.getDownPrice() == null || service.getDownPrice() <= 0) {
            return CONFLICT("Invalid down price");
        }

        // 🧠 Set system-managed fields
        service.setOrderNumber(orderNumber);
        service.setProvider(provider);
        service.setDeleted(false);

        servicesRepository.save(service);

        return SUCCESS("Service created successfully");
    }

    @Override
    public ResponseEntity<?> updateService(com.flex.service_module.impl.entities.Service service,
            HttpServletRequest request) {
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

        com.flex.service_module.impl.entities.Service existing = servicesRepository
                .findById(service.getId()).orElse(null);

        if (existing == null || existing.isDeleted()) {
            return CONFLICT("Service not found");
        }

        // 🔐 Ownership check
        if (!existing.getProvider().getId().equals(provider.getId())) {
            return CONFLICT("Unauthorized access");
        }

        boolean updated = false;

        // ✅ Update name
        if (service.getName() != null
                && !service.getName().trim().isEmpty()
                && !service.getName().equals(existing.getName())) {

            if (servicesRepository.existsByNameAndProvider_IdAndDeletedIsFalseAndIdNot(
                    service.getName(), provider.getId(), existing.getId())) {
                return CONFLICT("Service name already exists");
            }

            existing.setName(service.getName());
            updated = true;
        }

        // ✅ Update description
        if (service.getDescription() != null
                && !service.getDescription().trim().isEmpty()
                && !service.getDescription().equals(existing.getDescription())) {

            existing.setDescription(service.getDescription());
            updated = true;
        }

        // ✅ Update service time
        if (service.getServiceTime() != null
                && !service.getServiceTime().equals(existing.getServiceTime())) {

            existing.setServiceTime(service.getServiceTime());
            updated = true;
        }

        // ✅ Update total price
        if (service.getTotalPrice() != null
                && !service.getTotalPrice().equals(existing.getTotalPrice())) {

            existing.setTotalPrice(service.getTotalPrice());
            updated = true;
        }

        // ✅ Update down price
        if (service.getDownPrice() != null
                && !service.getDownPrice().equals(existing.getDownPrice())) {

            existing.setDownPrice(service.getDownPrice());
            updated = true;
        }

        // ✅ Update order number
        if (service.getOrderNumber() != null && !service.getOrderNumber().equals(existing.getOrderNumber())) {

            int orderNumber = existing.getOrderNumber();

            List<com.flex.service_module.impl.entities.Service> serviceList = servicesRepository
                    .findAllByProvider_IdAndDeletedIsFalseOrderByOrderNumberAsc(provider.getId());

            if (!serviceList.isEmpty()) {
                if (service.getOrderNumber() <= serviceList.getLast().getOrderNumber()) {
                    com.flex.service_module.impl.entities.Service orderedService = servicesRepository
                            .findByProvider_IdAndOrderNumberAndDeletedIsFalse(provider.getId(),
                                    service.getOrderNumber());

                    if (orderedService == null) {
                        orderNumber = service.getOrderNumber();
                    } else {
                        int temp = orderedService.getOrderNumber();
                        orderedService.setOrderNumber(orderNumber);
                        servicesRepository.save(orderedService);
                        orderNumber = temp;
                    }

                    existing.setOrderNumber(orderNumber);
                    updated = true;
                }
            }
        }

        if (existing.isFreeService() != service.isFreeService()) {
            existing.setFreeService(service.isFreeService());
            updated = true;
        }

        if (!updated) {
            return SUCCESS("No changes detected");
        }

        servicesRepository.save(existing);

        return SUCCESS("Service updated successfully");
    }

    @Override
    public ResponseEntity<?> availableServicesForPoint(Integer servicePointId, HttpServletRequest request) {
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

        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(servicePointId);

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        List<com.flex.service_module.impl.entities.Service> allServices = servicesRepository
                .findAllByProvider_IdAndDeletedIsFalseOrderByOrderNumber(provider.getId());

        List<com.flex.service_module.impl.entities.Service> assignedServices = availableServiceRepository.servicesInPoint(servicePointId);

        if (!assignedServices.isEmpty()) {

            // remove unassigned services before first assigned service
            int assignedFirstOrderNumber = assignedServices.getFirst().getOrderNumber();

            List<com.flex.service_module.impl.entities.Service> assignedAndUnassigned = allServices
                    .subList(assignedFirstOrderNumber, allServices.size());

            List<Integer> unavailableServicesIds = assignedServices.stream()
                    .map(com.flex.service_module.impl.entities.Service::getId)
                .toList();

            List<com.flex.service_module.impl.entities.Service> availableServices = assignedAndUnassigned.stream().filter(
                s -> !unavailableServicesIds.contains(s.getId())).toList();

            return DATA(availableServices);
        } else {
            return DATA(allServices);
        }
    }

    @Override
    public ResponseEntity<?> assignedServicesForPoint(Integer servicePointId, HttpServletRequest request) {
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

        ServicePoint servicePoint = servicePointRepository.findByIdAndDeletedIsFalse(servicePointId);

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        List<com.flex.service_module.impl.entities.Service> assignedServices
                = availableServiceRepository.servicesInPoint(servicePointId);

        return DATA(assignedServices);
    }

    @Override
    public ResponseEntity<?> assignedPointsForService(Integer service, HttpServletRequest request) {
        log.info(request.getRequestURI());

        if (!servicesRepository.existsByIdAndDeletedIsFalse(service)) {
            return CONFLICT("Service not found");
        }

        return DATA(availableServiceRepository.findPointsByServiceId(service));
    }

    @Override
    public ResponseEntity<?> assignServicesForPoint(AssignServiceToPoint assignServiceToPoint,
            HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServicePoint servicePoint = servicePointRepository
                .findByIdAndDeletedIsFalse(assignServiceToPoint.getPointId());

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        List<com.flex.service_module.impl.entities.Service> availableServices = availableServiceRepository
                .servicesInPoint(servicePoint.getId());

        com.flex.service_module.impl.entities.Service service = servicesRepository
                .findByIdAndDeletedIsFalse(assignServiceToPoint.getServiceId());

        if (service == null) {
            return CONFLICT("Service not found");
        }

        if (!availableServices.isEmpty()) {
            boolean orderFine = false;
            int lastOrderNumber = service.getOrderNumber();

            // order number must 6 last number must 5
            if (lastOrderNumber - 1 == availableServices.getLast().getOrderNumber()) {
                orderFine = true;
            }

            if (!orderFine) {
                return CONFLICT("You must stick with the order");
            }
        }

        AvailableService availableService = AvailableService.builder()
                .service(service)
                .servicePoint(servicePoint)
                .build();

        availableServiceRepository.save(availableService);

        return SUCCESS("Service assigned successfully");
    }

    @Override
    public ResponseEntity<?> removeServicesFromPoint(AssignServiceToPoint assignServiceToPoint,
            HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServicePoint servicePoint = servicePointRepository
                .findByIdAndDeletedIsFalse(assignServiceToPoint.getPointId());

        if (servicePoint == null) {
            return CONFLICT("Service point not found");
        }

        com.flex.service_module.impl.entities.Service service = servicesRepository
                .findByIdAndDeletedIsFalse(assignServiceToPoint.getServiceId());

        if (service == null) {
            return CONFLICT("Service not found");
        }

        AvailableService availableService = availableServiceRepository.availableServiceV1(service.getId(),
                servicePoint.getId());

        if (availableService == null) {
            return CONFLICT("This service is not assigned to this point");
        }

        availableServiceRepository.delete(availableService);

        return SUCCESS("Service removed successfully");
    }

    @Override
    public ResponseEntity<?> getAllServices(Pagination pagination, HttpServletRequest request) {
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

        Sort sort = Sort.by(Sorting.getSort(pagination.getSort()));

        Pageable pageable = PageRequest.of(
                pagination.getPage(),
                pagination.getSize(),
                sort);

        Page<com.flex.service_module.impl.entities.Service> result = servicesRepository.findAllWithSearch(
                provider.getId(),
                pagination.getSearchText() == null
                        ? null
                        : pagination.getSearchText().trim(),
                pageable);

        return DATA(
                result.map(s -> ServiceViewDTO.builder()
                        .id(s.getId())
                        .orderNumber(s.getOrderNumber())
                        .name(s.getName())
                        .description(s.getDescription())
                        .serviceTime(s.getServiceTime())
                        .fServiceTime(servicesServiceHelper.formatDuration(s.getServiceTime()))
                        .totalPrice(s.getTotalPrice())
                        .downPrice(s.getDownPrice())
                        .build()));
    }

    @Override
    public ResponseEntity<?> getAllServices(HttpServletRequest request) {
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

        return DATA(servicesRepository.getServicesDropdown(provider.getId()));
    }

    @Override
    public ResponseEntity<?> deleteService(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI());

        com.flex.service_module.impl.entities.Service service = servicesRepository.findByIdAndDeletedIsFalse(id);

        if (service == null) {
            return CONFLICT("Service not found");
        }

        List<com.flex.service_module.impl.entities.Service> serviceList = servicesRepository
                .findAllByProvider_IdAndDeletedIsFalseOrderByOrderNumberAsc(service.getProvider().getId());

        int orderNumber = service.getOrderNumber();

        List<com.flex.service_module.impl.entities.Service> orderedServices = serviceList
                .subList(orderNumber, serviceList.size());

        if (!orderedServices.isEmpty()) {
            orderNumber = orderNumber - 1;
            List<com.flex.service_module.impl.entities.Service> reorderedServices = new ArrayList<>();

            for (com.flex.service_module.impl.entities.Service reorder : orderedServices) {
                reorder.setOrderNumber(orderNumber + 1);

                reorderedServices.add(reorder);
            }

            servicesRepository.saveAll(reorderedServices);
        }

        // todo: before delete, check pending jobs are available
        // if yes, then noLongerAvailable is true
        // else deleted is true

        service.setDeleted(true);

        servicesRepository.save(service);

        return SUCCESS("Service deleted successfully");
    }
}
