package com.flex.notification_module.impl.services;

import com.flex.common_module.http.ReturnResponse;
import com.flex.notification_module.api.services.NotificationTypeService;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.flex.common_module.http.ReturnResponse.*;
import static com.flex.common_module.http.ReturnResponse.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class NotificationTypeServiceImpl implements NotificationTypeService {

    private final NotificationTypeRepository notificationTypeRepository;

    @Override
    public ResponseEntity<?> getAll(HttpServletRequest request) {
        log.info(request.getRequestURI());

        return ReturnResponse.DATA(notificationTypeRepository.findAll());
    }
}
