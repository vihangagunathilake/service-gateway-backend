package com.flex.notification_module.impl.services;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.constants.NotificationConstants;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.notification_module.api.services.NotificationService;
import com.flex.notification_module.impl.repositories.NotificationTypeRepository;
import com.flex.notification_module.impl.repositories.RoleNotificationRepository;
import com.flex.notification_module.impl.repositories.UserNotificationRepository;
import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.flex.common_module.http.ReturnResponse.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class NotificationServiceImpl implements NotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;

    private final JobRepository jobRepository;
    private final RoleNotificationRepository roleNotificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    @Override
    public ResponseEntity<?> notify(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user != null) {
            // get all accessible notifications
            List<String> notificationTypes = roleNotificationRepository.getRoleNotificationTypes(user.getRole().getId());

            if (notificationTypes == null || notificationTypes.isEmpty()) {
                return DATA(0);
            }

            return DATA(userNotificationRepository.getUserNotificationCount(user.getId(), notificationTypes).size());
        }

        return DATA(0);
    }

    @Override
    public ResponseEntity<?> notifyNoAgent(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user != null) {

            return DATA(userNotificationRepository.getUserNotificationsCountByUserIdAndType(user.getId(),
                    NotificationConstants.NO_AGENT_FOR_JOB, CommonMethods.getCurrentDate()));
        }

        return DATA(0);
    }

    @Override
    public ResponseEntity<?> notified(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user != null) {

            List<Integer> notViewedNotificationIds = userNotificationRepository
                    .getNotViewedNotificationIds(user.getId());

            List<String> crucialTypes = notificationTypeRepository.getCrucialNotificationTypes();

            userNotificationRepository.markAsViewButNotForCrucial(notViewedNotificationIds, crucialTypes);

            List<String> notificationTypes = roleNotificationRepository
                    .getRoleNotificationTypes(user.getRole().getId());

            if (notificationTypes == null || notificationTypes.isEmpty()) {
                return DATA(0);
            }

            return DATA(userNotificationRepository.getUserNotificationCount(user.getId(), notificationTypes).size());
        }
        return DATA(0);
    }

    @Override
    public ResponseEntity<?> timeoutJobs(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user != null) {
            return DATA(jobRepository.getJobCountByStatusAndCenter(JobStatus.TIMEOUT,
                    user.getServiceProvider().getId()));
        }
        return DATA(0);
    }
}
