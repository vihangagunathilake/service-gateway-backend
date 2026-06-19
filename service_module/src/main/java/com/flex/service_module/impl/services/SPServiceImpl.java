package com.flex.service_module.impl.services;

import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.service_module.api.http.responses.SPProfile;
import com.flex.service_module.api.services.SPService;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.ServiceProviderRepository;
import com.flex.service_module.impl.services.helpers.SPServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;

import static com.flex.common_module.http.ReturnResponse.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class SPServiceImpl implements SPService {

    private final SPServiceHelper spServiceHelper;

    private final ServiceProviderRepository serviceProviderRepository;

    @Override
    public ResponseEntity<?> serviceProviderProfile(HttpServletRequest request) {
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

        return DATA(
                SPProfile.builder()
                        .id(provider.getId())
                        .name(provider.getName())
                        .regNo(provider.getProviderId())
                        .email(provider.getEmail())
                        .contact(provider.getContact())
                        .address(provider.getAddress())
                        .website(provider.getWebsite())
                        .status(provider.isActive() ? "Active" : "Restricted")
                        .joinDate(new SimpleDateFormat("MMM dd, yyyy").format(provider.getAddedTime()))
                        .description(provider.getDescription())
                        .profile(provider.getProfileImage())
                        .cover(provider.getCoverPhoto())
                        .build()
        );
    }

    @Override
    public ResponseEntity<?> editServiceProvider(ServiceProvider serviceProvider, HttpServletRequest request) {
        log.info(request.getRequestURI() + " body: " + serviceProvider);

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        ServiceProvider existing = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (existing == null) {
            return CONFLICT("Service provider not found");
        }

        if (spServiceHelper.isValidAndDifferent(serviceProvider.getName(), existing.getName())) {
            existing.setName(serviceProvider.getName());
        }

        if (spServiceHelper.isValidAndDifferent(serviceProvider.getEmail(), existing.getEmail())) {
            existing.setEmail(serviceProvider.getEmail());
        }

        if (spServiceHelper.isValidAndDifferent(serviceProvider.getContact(), existing.getContact())) {
            existing.setContact(serviceProvider.getContact());
        }

        if (spServiceHelper.isValidAndDifferent(serviceProvider.getAddress(), existing.getAddress())) {
            existing.setAddress(serviceProvider.getAddress());
        }

        if (spServiceHelper.isValidAndDifferent(serviceProvider.getWebsite(), existing.getWebsite())) {
            existing.setWebsite(serviceProvider.getWebsite());
        }

        if (spServiceHelper.isValidAndDifferent(serviceProvider.getDescription(), existing.getDescription())) {
            existing.setDescription(serviceProvider.getDescription());
        }

        serviceProviderRepository.save(existing);

        return SUCCESS("Service provider updated");
    }

    @Override
    public ResponseEntity<?> uploadSPProfileImage(MultipartFile multipartFile, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        ServiceProvider serviceProvider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (serviceProvider == null) {
            return CONFLICT("Service provider not found");
        }

        try {
            serviceProvider.setProfileImage(spServiceHelper
                    .saveProviderProfileImage(multipartFile, serviceProvider.getId()));

            serviceProviderRepository.save(serviceProvider);

        } catch (IOException e) {
            e.printStackTrace();
            return CONFLICT("Profile image upload failed");
        }

        return SUCCESS("Save changes");
    }

    @Override
    public ResponseEntity<?> uploadSPCoverPhoto(MultipartFile multipartFile, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        ServiceProvider serviceProvider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (serviceProvider == null) {
            return CONFLICT("Service provider not found");
        }

        try {
            serviceProvider.setCoverPhoto(spServiceHelper
                    .saveProviderCoverImage(multipartFile, serviceProvider.getId()));

            serviceProviderRepository.save(serviceProvider);

        } catch (IOException e) {
            e.printStackTrace();
            return CONFLICT("Cover image upload failed");
        }

        return SUCCESS("Save changes");
    }
}
