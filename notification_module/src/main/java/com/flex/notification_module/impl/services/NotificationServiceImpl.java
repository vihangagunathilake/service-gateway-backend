package com.flex.notification_module.impl.services;

import com.flex.common_module.constants.NotificationConstants;
import com.flex.common_module.http.ReturnResponse;
import com.flex.common_module.http.pagination.Pagination;
import com.flex.common_module.http.pagination.Sorting;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.notification_module.api.http.DTO.NotificationAccessList;
import com.flex.notification_module.api.http.DTO.UserNotificationsList;
import com.flex.notification_module.api.http.responses.NotificationSummary;
import com.flex.notification_module.api.services.NotificationService;
import com.flex.notification_module.impl.entities.NotificationAccess;
import com.flex.notification_module.impl.entities.UserNotification;
import com.flex.notification_module.impl.repositories.NotificationAccessRepository;
import com.flex.notification_module.impl.repositories.NotificationRepository;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.notification_module.impl.services.helpers.NotificationServiceHelper;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.flex.common_module.http.ReturnResponse.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationAccessRepository notificationAccessRepository;
    private final UserRepository userRepository;

    private final NotificationServiceHelper notificationServiceHelper;

    @Override
    public ResponseEntity<?> notifications(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return ReturnResponse.CONFLICT("User not found");
        }

        List<NotificationAccessList> notificationAccessList = notificationAccessRepository
                .findAccessListByUserId(userClaims.getUserId());

        List<UserNotificationsList> summary = new ArrayList<>();

        for (NotificationAccessList notificationAccess : notificationAccessList) {
            List<UserNotificationsList> userNotificationsLists = userNotificationRepository
                    .getUserNotificationsListByUserId(userClaims.getUserId(), notificationAccess.getAccessName());

            summary.addAll(userNotificationsLists);
        }

        return ReturnResponse.DATA(summary);
    }

    @Override
    public ResponseEntity<?> userNotifications(Pagination pagination, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return ReturnResponse.CONFLICT("User not found");
        }

        User existingUser = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (existingUser == null) {
            return ReturnResponse.CONFLICT("User not found");
        }

        Sort sort = Sort.by(Sorting.getSort(pagination.getSort()));

        Pageable pageable = PageRequest.of(
                pagination.getPage(),
                pagination.getSize(),
                sort
        );

        return ReturnResponse.DATA(notificationRepository
                .notifications(userClaims.getUserId(), pageable).getContent());
    }

    @Override
    public ResponseEntity<?> markAsRead(Integer userNotificationId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserNotification userNotification = userNotificationRepository.getUserNotificationByIdAndViewedIsFalse(
                userNotificationId
        );

        userNotification.setViewed(true);
        userNotificationRepository.save(userNotification);

        return SUCCESS(null);
    }
}
