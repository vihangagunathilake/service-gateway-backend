package com.flex.service_module.api.services;

import com.flex.service_module.impl.entities.ServiceProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface SPService {

    ResponseEntity<?> serviceProviderProfile(HttpServletRequest request);

    ResponseEntity<?> editServiceProvider(ServiceProvider serviceProvider, HttpServletRequest request);

    ResponseEntity<?> uploadSPProfileImage(MultipartFile multipartFile, HttpServletRequest request);

    ResponseEntity<?> uploadSPCoverPhoto(MultipartFile multipartFile, HttpServletRequest request);
}
