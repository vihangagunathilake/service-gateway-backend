package com.flex.user_module.impl.services;

import com.flex.common_module.CommonMethods;
import com.flex.common_module.http.pagination.Pagination;
import com.flex.common_module.http.pagination.Sorting;
import com.flex.common_module.mails.services.EmailService;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.CryptoUtil;
import com.flex.common_module.security.utils.HashUtil;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.job_module.api.http.DTO.JobAtPointDetails;
import com.flex.job_module.impl.repositories.JobTrackRepository;
import com.flex.user_module.api.http.responses.*;
import com.flex.job_module.constants.JobStatus;
import com.flex.job_module.impl.entities.Customer;
import com.flex.job_module.impl.entities.Job;
import com.flex.job_module.impl.entities.JobAtPoint;
import com.flex.job_module.impl.repositories.JobAtPointRepository;
import com.flex.job_module.impl.repositories.JobRepository;
import com.flex.service_module.impl.entities.Cluster;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServicePoint;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.ClusterRepository;
import com.flex.service_module.impl.repositories.ServiceCenterRepository;
import com.flex.service_module.impl.repositories.ServiceProviderRepository;
import com.flex.user_module.api.DTO.CenterUsers;
import com.flex.user_module.api.http.requests.*;
import com.flex.user_module.api.services.UserService;
import com.flex.user_module.cache.RoleCacheService;
import com.flex.user_module.impl.entities.*;
import com.flex.user_module.impl.entities.RolePermissionAccess;
import com.flex.user_module.impl.repositories.*;
import com.flex.user_module.impl.services.helpers.UserServiceHelper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.flex.common_module.http.ReturnResponse.*;
import static com.flex.user_module.constants.UserConstant.*;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserServiceImpl implements UserService {

    private final JwtUtil jwtUtil;

    private final UserServiceHelper userServiceHelper;
    private final RoleCacheService roleCacheService;
    private final EmailService emailService;

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final UserStatusRepository userStatusRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionAccessRepository rolePermissionAccessRepository;
    private final AgentLoginRepository agentLoginRepository;
    private final UserPasswordTokenRepository userPasswordTokenRepository;

    private final JobRepository jobRepository;
    private final ClusterRepository clusterRepository;
    private final JobAtPointRepository jobAtPointRepository;
    private final AgentJobRepository agentJobRepository;
    private final JobTrackRepository jobTrackRepository;

    @Override
    @Transactional
    public ResponseEntity<?> register(Register register, HttpServletRequest request) throws MessagingException {
        log.info(request.getRequestURI(), "{} body - {}", register);

        if (serviceProviderRepository.existsByNameAndEmailAndDeletedIsFalse(
                register.getProvider(), register.getProviderEmail())) {
            return CONFLICT("Service provider already registered");
        }

        if (userRepository.existsByEmailAndDeletedIsFalse(register.getAdminEmail())) {
            return CONFLICT("User already registered");
        }

        // create permissions for admin
        List<Permission> permissions = permissionRepository.findAll();

        if (permissions.isEmpty()) {
            return CONFLICT("Permissions are empty for users");
        }

        String providerId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        // create service provider entity
        ServiceProvider serviceProvider = ServiceProvider.builder()
                .name(register.getProvider())
                .email(register.getProviderEmail())
                .providerId(providerId)
                .contact(register.getContact())
                .addedTime(new Date())
                .active(true)
                .build();

        ServiceProvider savedSP = serviceProviderRepository.save(serviceProvider);

        // create service center entity if has no other centers
        if (!register.isHasMultipleBranches()) {
            ServiceCenter serviceCenter = ServiceCenter.builder()
                    .name(serviceProvider.getName())
                    .build();

            serviceCenterRepository.save(serviceCenter);
        }

        // create admin entity
        Role admin = Role.builder().role("Admin")
                .serviceProvider(savedSP)
                .build();

        List<RolePermission> rolePermissions = permissions.stream().map(
                p -> RolePermission.builder().role(admin).permission(p).build()).collect(Collectors.toList());

        List<RolePermissionAccess> rolePermissionAccesses = rolePermissions.stream().map(
                rolePermission -> RolePermissionAccess.builder()
                        .all_permission(true)
                        .rolePermission(rolePermission)
                        .build())
                .toList();

        List<Permission> employeePermissions = permissionRepository.getAllByEmployeeIsTrue();

        if (employeePermissions.isEmpty()) {
            return CONFLICT("Employee permissions are empty");
        }

        Permission permitThis = permissionRepository.getPermissionByPermission("Permit This");

        if (permitThis == null) {
            return CONFLICT("Permit this permission is not found");
        }

        employeePermissions.add(permitThis);

        // create employee entity
        Role employee = Role.builder().role("Employee")
                .serviceProvider(savedSP)
                .employee(true)
                .build();

        List<RolePermission> employeeRolePermission = employeePermissions.stream().map(
                p -> RolePermission.builder().role(employee).permission(p).build()).toList();

        List<RolePermissionAccess> employeeRolePermissionAccesses = employeeRolePermission.stream().map(
                rolePermission -> RolePermissionAccess.builder()
                        .all_permission(true)
                        .rolePermission(rolePermission)
                        .build())
                .toList();

        // create user entity for admin
        User user = User.builder()
                .fName(register.getAdminFName())
                .lName(register.getAdminLName())
                .email(register.getAdminEmail())
                .role(admin)
                .serviceProvider(serviceProvider)
                .userType(ADMIN)
                .build();

        UserDetails userDetails = UserDetails.builder()
                .nic(CryptoUtil.encrypt(register.getNic()))
                .user(user)
                .addedTime(new Date())
                .build();

        String newToken = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        UserPasswordToken userPasswordToken = UserPasswordToken.builder()
                .token(newToken)
                .expireTime(null)
                .user(user)
                .used(false)
                .build();

        user.setPassword(HashUtil.hash(newToken));

        roleRepository.save(admin);
        rolePermissionRepository.saveAll(rolePermissions);
        rolePermissionAccessRepository.saveAll(rolePermissionAccesses);

        roleRepository.save(employee);
        rolePermissionRepository.saveAll(employeeRolePermission);
        rolePermissionAccessRepository.saveAll(employeeRolePermissionAccesses);

        userRepository.save(user);
        userDetailsRepository.save(userDetails);

        userPasswordTokenRepository.save(userPasswordToken);

        emailService.newPassword(user.getEmail(), newToken, user.getFName());

        return SUCCESS("Registration Completed. Please login");
    }

    // todo create a service for customer registration - google OAuth

    @Override
    public ResponseEntity<?> login(Login login, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} body - {}", login);

        User user = userRepository.findByEmailAndDeletedIsFalse(login.getUsername());

        if (user == null) {
            return CONFLICT("Invalid username");
        }

        if (!user.isResetPassword()) {
            Map<String, Object> claims = new HashMap<>();

            claims.put("user", user.getId());
            claims.put("type", user.getUserType());
            claims.put("provider", user.getServiceProvider().getProviderId());
            claims.put("center", user.getServiceCenter() != null ? user.getServiceCenter().getId() : null);

            String token = jwtUtil.generateToken(claims, user.getEmail());

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .refreshToken(null)
                    .password(user.isResetPassword())
                    .forgot(user.isForgotPassword())
                    .build();

            UserLogin userLogin = UserLogin.builder()
                    .loginTime(new Date())
                    .token(token)
                    .userId(user.getId())
                    .build();

            userLoginRepository.save(userLogin);

            user.setPassword(HashUtil.hash(login.getPassword()));
            userRepository.save(user);

            return DATA(response);
        }

        if (!HashUtil.checkEncrypted(login.getPassword(), user.getPassword())) {
            return CONFLICT("Invalid password");
        }

        if (user.getUserType() != ADMIN && user.getRole() == null) {
            return CONFLICT("Invalid role");
        }

        // if has any previous login with no logout(logged in and close the browser
        // without logout)
        // , logout from every login
        userServiceHelper.logoutFromPreviousLogins(user.getId());

        Map<String, Object> claims = new HashMap<>();

        claims.put("user", user.getId());
        claims.put("type", user.getUserType());
        claims.put("provider", user.getServiceProvider().getProviderId());
        claims.put("center", user.getServiceCenter() != null ? user.getServiceCenter().getId() : null);

        String token = jwtUtil.generateToken(claims, user.getEmail());
        String refreshToken = jwtUtil.refreshToken(claims, user.getEmail());

        UserLogin userLogin = UserLogin.builder()
                .loginTime(new Date())
                .token(token)
                .userId(user.getId())
                .build();

        userLoginRepository.save(userLogin);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .password(user.isResetPassword())
                .forgot(user.isForgotPassword())
                .userType(user.getUserType())
                .serviceCenter(user.getUserType() == 2 ? user.getServiceCenter().getId() : null)
                .build();

        return DATA(response);
    }

    @Override
    public ResponseEntity<?> newPassword(ChangePassword changePassword, HttpServletRequest request) {
        log.info(request.getRequestURI());

        User user = userRepository.findByEmailAndDeletedIsFalse(changePassword.getEmail());

        if (user == null) {
            return CONFLICT("This email does not exist");
        }

        if (changePassword.isForgot()) {

            List<UserPasswordToken> resetPasswordTokens = userPasswordTokenRepository.getNonExpiredUserPasswordTokens(
                    user.getId(), changePassword.getEmailString(), CommonMethods.getCurrentDateTime());

            if (resetPasswordTokens.isEmpty()) {
                return CONFLICT("Reset Password Tokens not found");
            }

            if (HashUtil.checkEncrypted(changePassword.getNewPassword(), user.getPassword())) {
                return CONFLICT("Old and new passwords can not be the same");
            }

            resetPasswordTokens.forEach(token -> token.setUsed(true));

            userPasswordTokenRepository.saveAll(resetPasswordTokens);

            user.setPassword(HashUtil.hash(changePassword.getNewPassword()));
            user.setForgotPassword(false);
            user.setResetPassword(true);
            userRepository.save(user);

            return SUCCESS("Password Reset Completed");
        } else {
            UserPasswordToken userPasswordToken = userPasswordTokenRepository.findTokenByUserId(
                    user.getId(),
                    changePassword.getEmailString());

            if (userPasswordToken == null) {
                return CONFLICT("This email does not registered");
            }

            userPasswordToken.setUsed(true);
            userPasswordTokenRepository.save(userPasswordToken);

            user.setPassword(HashUtil.hash(changePassword.getNewPassword()));
            user.setResetPassword(true);

            userRepository.save(user);

            return SUCCESS("Password changed successfully");
        }
    }

    @Override
    public ResponseEntity<?> forgetPassword(ChangePassword changePassword, HttpServletRequest request)
            throws MessagingException {
        log.info(request.getRequestURI());

        User user = userRepository.findByEmailAndDeletedIsFalse(changePassword.getEmail());

        if (user == null) {
            return CONFLICT("This email does not exist");
        }

        List<UserPasswordToken> resetPasswordTokens = userPasswordTokenRepository.getNonExpiredUserPasswordTokens(
                user.getId(), changePassword.getNewPassword(), CommonMethods.getCurrentDateTime());

        if (!resetPasswordTokens.isEmpty()) {
            return SUCCESS("Reset token already sent. Please check your email");
        }

        String newToken = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        user.setResetPassword(false);
        user.setForgotPassword(true);

        userRepository.save(user);

        UserPasswordToken userPasswordToken = UserPasswordToken.builder()
                .token(newToken)
                .expireTime(CommonMethods.getCurrentDateTime().plusMinutes(5))
                .user(user)
                .build();

        userPasswordTokenRepository.save(userPasswordToken);

        emailService.sendPasswordResetEmail(user.getEmail(), newToken, user.getFName());

        return SUCCESS("Password Reset Request sent. Please check your email");
    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        if (user.getUserType() == EMPLOYEE) {
            AgentLogin agentLogin = agentLoginRepository.getAgentLogin(user.getId());

            if (agentLogin != null) {

                List<AgentJob> agentJobs = agentJobRepository.getServingJobByAgentInPoint(user.getId(),
                        agentLogin.getServicePoint().getId(),
                        CommonMethods.getCurrentDate());

                if (!agentJobs.isEmpty()) {
                    return CONFLICT("Please end the job before leaving");
                }

                agentLogin.setLogoutDate(CommonMethods.getCurrentDate());
                agentLogin.setLogoutTime(CommonMethods.getCurrentTime());

                agentLoginRepository.save(agentLogin);

            }
        }

        userServiceHelper.logoutFromPreviousLogins(user.getId());
        roleCacheService.evictPermissionsCache(user.getId(), user.getRole().getRole());

        return SUCCESS("Successfully logout");
    }

    @Override
    public ResponseEntity<?> headerData(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        ServicePoint loggedInPoint = null;

        if (user.getUserType() == EMPLOYEE) {
            AgentLogin agentLogin = agentLoginRepository.getAgentLogin(user.getId());

            if (agentLogin != null) {
                loggedInPoint = agentLogin.getServicePoint();
            }
        }

        return DATA(
                HeaderData.builder()
                        .userType(user.getUserType() == 0 ? ADMIN_T
                                : user.getUserType() == 1 ? USER_T
                                        : EMPLOYEE_T)
                        .email(user.getEmail())
                        .providerId(user.getServiceProvider().getProviderId())
                        .serviceCenter(user.getServiceProvider().getName())
                        .userName(user.getFName())
                        .image(user.getProfileImageUrl())
                        .loggedInPoint(loggedInPoint != null ? loggedInPoint.getName() : null)
                        .loggedInPointId(loggedInPoint != null ? loggedInPoint.getId() : null)
                        .build());
    }

    @Override
    public ResponseEntity<?> permissions(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        if (user.getRole() == null || user.getRole().isDeleted()) {
            return CONFLICT("User has no role");
        }

        List<RolePermission> permissions = rolePermissionRepository
                .getAllRolePermissions(user.getRole().getId());

        if (permissions.isEmpty()) {
            return CONFLICT("No permissions for the role");
        }

        List<Integer> permissionIds = permissions.stream().map(
                r -> r.getPermission().getId()).collect(Collectors.toList());

        return DATA(permissionRepository.getPermissionsByIds(permissionIds));
    }

    @Override
    public ResponseEntity<?> permissionsWithAccess(HttpServletRequest request) {
        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        if (user.getRole() == null || user.getRole().isDeleted()) {
            return CONFLICT("User has no role");
        }

        return DATA(userRepository.getUserPermissionAccess(user.getId()));
    }

    @Override
    public ResponseEntity<?> employeeRegister(EmployeeRegister employeeRegister, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} body - {}", employeeRegister);

        String validationStatus = userServiceHelper
                .employeeRegisterValidation(employeeRegister);

        if (!validationStatus.equals("success")) {
            return CONFLICT(validationStatus);
        }

        UserDetails prevUserDetails = userDetailsRepository.findByNic(
                CryptoUtil.encrypt(employeeRegister.getNic()));

        if (prevUserDetails != null
                && !prevUserDetails.getUser().isDeleted()
                && prevUserDetails.getUser().getUserType() == EMPLOYEE) {
            return CONFLICT("Employee already exist by this NIC");
        }

        ServiceProvider serviceProvider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(employeeRegister.getProviderId());

        if (serviceProvider == null) {
            return CONFLICT("Invalid service provider");
        }

        User user = User.builder()
                .fName(employeeRegister.getFName())
                .lName(employeeRegister.getLName())
                .email(employeeRegister.getEmail())
                .build();

        UserDetails userDetails = UserDetails.builder()
                .nic(CryptoUtil.encrypt(employeeRegister.getNic()))
                .contact(CryptoUtil.encrypt(employeeRegister.getContact()))
                .addedTime(new Date())
                .user(user)
                .build();

        UserStatus userStatus = UserStatus.builder()
                .user(user)
                .providerApproved(false)
                .build();

        userRepository.save(user);
        userDetailsRepository.save(userDetails);
        userStatusRepository.save(userStatus);

        return SUCCESS("Registration Completed, Please wait to provider's confirmation.");
    }

    @Override
    public ResponseEntity<?> getAllUsers(Pagination pagination, HttpServletRequest request) {
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

        Sort sort = Sort.by(Sorting.getSort(pagination.getSort()));

        Pageable pageable = PageRequest.of(
                pagination.getPage(),
                pagination.getSize(),
                sort);

        String search = "";

        if (pagination.getSpecialSearchOne() != null && !pagination.getSpecialSearchOne().isEmpty()) {
            search = CryptoUtil.encrypt(pagination.getSpecialSearchOne());
        } else if (pagination.getSpecialSearchTwo() != null && !pagination.getSpecialSearchTwo().isEmpty()) {
            search = CryptoUtil.encrypt(pagination.getSpecialSearchTwo());
        } else if (pagination.getSearchText() != null && !pagination.getSearchText().isEmpty()) {
            search = pagination.getSearchText();
        }

        return DATA(
                userRepository.findAllByServiceProvider(
                        serviceProvider.getId(),
                        search,
                        userClaims.getUserId(),
                        pageable).getContent());
    }

    @Override
    public ResponseEntity<?> employeeAssign(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI());

        User user = userRepository.findByIdAndDeletedIsFalse(id);

        if (user == null) {
            return CONFLICT("User not found");
        }

        UserStatus pendingUsers = userStatusRepository.findByUserIdAndProviderApprovedIsFalse(id);

        if (pendingUsers == null) {
            return CONFLICT("This user is already approved");
        }

        pendingUsers.setProviderApproved(true);

        userStatusRepository.save(pendingUsers);

        return SUCCESS("User has approved");
    }

    @Override
    public ResponseEntity<?> employeeReject(Integer id, HttpServletRequest request) {
        log.info(request.getRequestURI());

        User user = userRepository.findByIdAndDeletedIsFalse(id);

        if (user == null) {
            return CONFLICT("User not found");
        }

        UserStatus pendingUsers = userStatusRepository.findByUserIdAndProviderApprovedIsFalse(id);

        pendingUsers.setProviderApproved(false);

        userStatusRepository.save(pendingUsers);

        return SUCCESS("User has rejected");
    }

    @Override
    public ResponseEntity<?> decryptString(DecryptValue value, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        if (!userRepository.existsByIdAndDeletedIsFalse(userClaims.getUserId())) {
            return CONFLICT("User not found");
        }

        return DATA(CryptoUtil.decrypt(value.getKey()));
    }

    @Override
    @Transactional
    public ResponseEntity<?> addUser(AddUser addUser, HttpServletRequest request) throws MessagingException {
        log.info(request.getRequestURI(), "{} body - {}", addUser);

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        ServiceProvider serviceProvider = serviceProviderRepository
                .findByProviderIdAndDeletedIsFalse(userClaims.getProvider());

        if (serviceProvider == null) {
            return CONFLICT("Service provider not found");
        }

        Role role;

        if (addUser.getUserType() != 2) {
            role = roleRepository.findByIdAndDeletedIsFalse(addUser.getRoleId());
        } else {
            role = roleRepository.findByServiceProvider_IdAndEmployeeIsTrue(serviceProvider.getId());
        }

        if (role == null) {
            return CONFLICT("Role not found");
        }

        if (userRepository.existsByEmailAndDeletedIsFalse(CryptoUtil.encrypt(addUser.getEmail()))) {
            return CONFLICT("Email already exists");
        }

        if (userDetailsRepository.existsByContact(CryptoUtil.encrypt(addUser.getContact()))) {
            return CONFLICT("Contact already exists");
        }

        if (userDetailsRepository.existsByNic(CryptoUtil.encrypt(addUser.getNic()))) {
            return CONFLICT("Nic already exists");
        }

        ServiceCenter serviceCenter = null;

        if (addUser.getServiceCenterId() != null) {
            serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(addUser.getServiceCenterId());
        }

        User user = User.builder()
                .fName(addUser.getFirstName())
                .lName(addUser.getLastName())
                .email(addUser.getEmail())
                .role(role)
                .serviceCenter(serviceCenter)
                .serviceProvider(serviceProvider)
                .userType(addUser.getUserType())
                .build();

        // adding role notification is happening in notification_module
        // this is added to prevent circular dependency
        // publisher.publishEvent(
        // new UserCreatedEvent(
        // user
        // )
        // );

        UserDetails userDetails = UserDetails.builder()
                .user(user)
                .nic(addUser.getNic() != null && !addUser.getNic().isEmpty()
                        ? CryptoUtil.encrypt(addUser.getNic())
                        : null)
                .contact(addUser.getNic() != null && !addUser.getNic().isEmpty()
                        ? CryptoUtil.encrypt(addUser.getContact())
                        : null)
                .addedTime(new Date())
                .build();

        UserStatus userStatus = UserStatus
                .builder()
                .user(user)
                .providerApproved(true)
                .build();

        String newToken = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        UserPasswordToken userPasswordToken = UserPasswordToken.builder()
                .token(newToken)
                .expireTime(null)
                .user(user)
                .used(false)
                .build();

        userRepository.save(user);
        userDetailsRepository.save(userDetails);
        userStatusRepository.save(userStatus);
        userPasswordTokenRepository.save(userPasswordToken);

        emailService.newPassword(user.getEmail(), newToken, user.getFName());

        return SUCCESS("Registration Completed");
    }

    @Override
    public ResponseEntity<?> uploadProfileImage(Integer userId, MultipartFile profile, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        try {
            user.setProfileImageUrl(userServiceHelper.saveUserProfileImage(profile, user.getId()));

            userRepository.save(user);

        } catch (IOException e) {
            e.printStackTrace();
            return CONFLICT("Profile image upload failed");
        }

        return SUCCESS("Save changes");
    }

    @Override
    public ResponseEntity<?> uploadCoverImage(Integer userId, MultipartFile cover, HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        try {
            user.setCoverImageUrl(userServiceHelper.saveUserCoverImage(cover, user.getId()));

            userRepository.save(user);

        } catch (IOException e) {
            e.printStackTrace();
            return CONFLICT("Cover image upload failed");
        }

        return SUCCESS("Save changes");
    }

    @Override
    public ResponseEntity<?> getUser(Integer userId, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} user - {}", userId);

        User user = userRepository.findByIdAndDeletedIsFalse(userId);

        if (user == null) {
            return CONFLICT("User not found");
        }

        UserDetails userDetails = userDetailsRepository.findByUser_id(user.getId());

        return DATA(
                UserData.builder()
                        .userId(user.getId())
                        .fName(user.getFName())
                        .lName(user.getLName())
                        .email(user.getEmail())
                        .userType(user.getUserType())
                        .roleId(user.getRole().getId())
                        .serviceCenterId(user.getServiceCenter() != null ? user.getServiceCenter().getId() : null)
                        .nic(userDetails.getNic() != null ? CryptoUtil.decrypt(userDetails.getNic()) : null)
                        .contact(userDetails.getContact() != null ? CryptoUtil.decrypt(userDetails.getContact()) : null)
                        .build());
    }

    @Override
    public ResponseEntity<?> userProfileData(HttpServletRequest request) {
        log.info(request.getRequestURI());

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        UserDetails userDetails = userDetailsRepository.findByUser_id(user.getId());

        return DATA(
                UserData.builder()
                        .userId(user.getId())
                        .fName(user.getFName())
                        .lName(user.getLName())
                        .email(user.getEmail())
                        .userType(user.getUserType())
                        .roleId(user.getRole().getId())
                        .serviceCenterId(user.getServiceCenter() != null ? user.getServiceCenter().getId() : null)
                        .nic(userDetails.getNic() != null ? CryptoUtil.decrypt(userDetails.getNic()) : null)
                        .contact(userDetails.getContact() != null ? CryptoUtil.decrypt(userDetails.getContact()) : null)
                        .build());
    }

    @Override
    public ResponseEntity<?> userProfileDetails(HttpServletRequest request) {
        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User user = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        UserDetails userDetails = userDetailsRepository.findByUser_id(user.getId());

        String userType;

        if (user.getUserType() == 1) {
            userType = "ADMIN";
        } else if (user.getUserType() == 2) {
            userType = "EMPLOYEE";
        } else {
            userType = "USER";
        }

        return DATA(
                UserProfileData.builder()
                        .fName(user.getFName())
                        .lName(user.getLName())
                        .email(user.getEmail())
                        .userType(userType)
                        .role(user.getRole().getRole())
                        .serviceCenter(user.getServiceCenter() != null ? user.getServiceCenter().getName() : null)
                        .nic(userDetails.getNic() != null ? CryptoUtil.decrypt(userDetails.getNic()) : null)
                        .contact(userDetails.getContact() != null ? CryptoUtil.decrypt(userDetails.getContact()) : null)
                        .joinedDate(new SimpleDateFormat("MMM dd, yyyy").format(userDetails.getAddedTime()))
                        .profileImageUrl(user.getProfileImageUrl())
                        .coverImageUrl(user.getCoverImageUrl())
                        .build());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateUser(AddUser updateUser, HttpServletRequest request) {
        log.info(request.getRequestURI(), "{} body - {}", updateUser);

        if (updateUser.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User existingUser = userRepository.findByIdAndDeletedIsFalse(updateUser.getUserId());

        UserDetails existingUserDetails = userDetailsRepository.findByUser_id(updateUser.getUserId());

        if (updateUser.getFirstName() != null && !updateUser.getFirstName().isEmpty() &&
                !existingUser.getFName().equals(updateUser.getFirstName())) {
            existingUser.setFName(updateUser.getFirstName());
        }

        if (updateUser.getLastName() != null && !updateUser.getLastName().isEmpty() &&
                !existingUser.getLName().equals(updateUser.getLastName())) {
            existingUser.setLName(updateUser.getLastName());
        }
        if (updateUser.getEmail() != null && !updateUser.getEmail().isEmpty() &&
                !existingUser.getEmail().equals(updateUser.getEmail())) {
            existingUser.setEmail(updateUser.getEmail());
        }

        if (existingUser.getUserType() != updateUser.getUserType()) {
            existingUser.setUserType(updateUser.getUserType());
        }

        if (updateUser.getRoleId() != null
                && !existingUser.getRole().getId().equals(updateUser.getRoleId())
                && roleRepository.existsByIdAndDeletedIsFalse(updateUser.getRoleId())) {
            existingUser.setRole(new Role(updateUser.getRoleId()));

            // publisher.publishEvent(
            // new UserUpdatedEvent(
            // existingUser
            // )
            // );
        }

        if (existingUser.getServiceCenter() != null) {
            if (updateUser.getServiceCenterId() != null &&
                    !existingUser.getServiceCenter().getId().equals(updateUser.getServiceCenterId())
                    && serviceCenterRepository.existsByIdAndDeletedIsFalse(updateUser.getServiceCenterId())) {
                existingUser.setServiceCenter(new ServiceCenter(updateUser.getServiceCenterId()));
            }
        } else {
            if (serviceCenterRepository.existsByIdAndDeletedIsFalse(updateUser.getServiceCenterId())) {
                existingUser.setServiceCenter(new ServiceCenter(updateUser.getServiceCenterId()));
            }
        }

        if (updateUser.getNic() != null
                && !updateUser.getNic().isEmpty()) {
            existingUserDetails.setNic(CryptoUtil.encrypt(updateUser.getNic()));
        }

        if (updateUser.getContact() != null
                && !updateUser.getContact().isEmpty()) {
            existingUserDetails.setContact(CryptoUtil.encrypt(updateUser.getContact()));
        }

        userRepository.save(existingUser);
        userDetailsRepository.save(existingUserDetails);

        return SUCCESS("Update Completed");
    }

    @Override
    public ResponseEntity<?> updateUserProfile(AddUser updateUser, HttpServletRequest request) {
        log.info(request.getRequestURI() + " body: " + updateUser);

        UserClaims userClaims = JwtUtil.getClaimsFromToken(request);

        if (userClaims == null || userClaims.getUserId() == null) {
            return CONFLICT("User not found");
        }

        User existingUser = userRepository.findByIdAndDeletedIsFalse(userClaims.getUserId());

        if (existingUser == null) {
            return CONFLICT("User not found");
        }

        UserDetails existingUserDetails = userDetailsRepository.findByUser_id(userClaims.getUserId());

        if (updateUser.getFirstName() != null && !updateUser.getFirstName().isEmpty() &&
                !existingUser.getFName().equals(updateUser.getFirstName())) {
            existingUser.setFName(updateUser.getFirstName());
        }

        if (updateUser.getLastName() != null && !updateUser.getLastName().isEmpty() &&
                !existingUser.getLName().equals(updateUser.getLastName())) {
            existingUser.setLName(updateUser.getLastName());
        }
        if (updateUser.getEmail() != null && !updateUser.getEmail().isEmpty() &&
                !existingUser.getEmail().equals(updateUser.getEmail())) {
            existingUser.setEmail(updateUser.getEmail());
        }

        if (existingUser.getUserType() != updateUser.getUserType()) {
            existingUser.setUserType(updateUser.getUserType());
        }

        if (updateUser.getRoleId() != null
                && !existingUser.getRole().getId().equals(updateUser.getRoleId())
                && roleRepository.existsByIdAndDeletedIsFalse(updateUser.getRoleId())) {
            existingUser.setRole(new Role(updateUser.getRoleId()));
        }

        if (existingUser.getServiceCenter() != null) {
            if (updateUser.getServiceCenterId() != null &&
                    !existingUser.getServiceCenter().getId().equals(updateUser.getServiceCenterId())
                    && serviceCenterRepository.existsByIdAndDeletedIsFalse(updateUser.getServiceCenterId())) {
                existingUser.setServiceCenter(new ServiceCenter(updateUser.getServiceCenterId()));
            }
        } else {
            if (serviceCenterRepository.existsByIdAndDeletedIsFalse(updateUser.getServiceCenterId())) {
                existingUser.setServiceCenter(new ServiceCenter(updateUser.getServiceCenterId()));
            }
        }

        if (updateUser.getNic() != null
                && !updateUser.getNic().isEmpty()) {
            existingUserDetails.setNic(CryptoUtil.encrypt(updateUser.getNic()));
        }

        if (updateUser.getContact() != null
                && !updateUser.getContact().isEmpty()) {
            existingUserDetails.setContact(CryptoUtil.encrypt(updateUser.getContact()));
        }

        userRepository.save(existingUser);
        userDetailsRepository.save(existingUserDetails);

        return SUCCESS("Profile edited");
    }

    @Override
    public ResponseEntity<?> deleteUser(Integer id, HttpServletRequest request) {

        log.info(request.getRequestURI(), "{} user - {}", id);

        User user = userRepository.findByIdAndDeletedIsFalse(id);

        if (user == null) {
            return CONFLICT("User not found");
        }

        user.setDeleted(true);

        userRepository.save(user);

        UserDetails userDetails = userDetailsRepository.findByUser_id(user.getId());

        if (userDetails != null) {
            userDetailsRepository.delete(userDetails);
        }

        return SUCCESS("Deleted User");
    }

    @Override
    public ResponseEntity<?> assignEmployeesToCenters(EmployeeAssign employeeAssign, HttpServletRequest request) {
        log.info(request.getRequestURI());

        User user = userRepository.findByIdAndDeletedIsFalse(employeeAssign.getEmployeeId());

        if (user == null) {
            return CONFLICT("User not found");
        }

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(employeeAssign.getCenterId());

        if (serviceCenter == null) {
            return CONFLICT("ServiceCenter not found");
        }

        user.setServiceCenter(serviceCenter);

        userRepository.save(user);

        return SUCCESS("Employee assigned");
    }

    @Override
    public ResponseEntity<?> usersByCenter(Integer centerId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        ServiceCenter serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(centerId);

        if (serviceCenter == null) {
            return CONFLICT("ServiceCenter not found");
        }

        List<CenterUsers> centerUsers = userRepository.findUsersByServiceCenter(centerId);

        List<com.flex.user_module.api.http.responses.CenterUsers> centerUsersList = centerUsers.stream().map(
                u -> com.flex.user_module.api.http.responses.CenterUsers.builder()
                        .userId(u.getUserId())
                        .userName(u.getUserName())
                        .contact(CryptoUtil.decrypt(u.getContact()))
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .toList();

        return DATA(centerUsersList);
    }

    @Override
    public ResponseEntity<?> nonAssignedUsers(Integer centerId, HttpServletRequest request) {
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

        return DATA(userRepository.getNonAssignedUsers(serviceProvider.getId(), centerId));
    }

    @Override
    public ResponseEntity<?> removeUserFromCenter(Integer userId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        User user = userRepository.findByIdAndDeletedIsFalse(userId);

        if (user == null) {
            return CONFLICT("User not found");
        }

        user.setServiceCenter(null);

        userRepository.save(user);

        return SUCCESS("Removed User");
    }

    @Override
    public ResponseEntity<?> jobDetails(Integer jobId, HttpServletRequest request) {
        log.info(request.getRequestURI());

        Job job = jobRepository.getJobById(jobId);

        if (job == null) {
            return CONFLICT("Job not found");
        }

        Customer customer = job.getCustomer();

        Cluster cluster = null;

        if (job.getClusterId() != null) {
            cluster = clusterRepository.findByIdAndDeletedIsFalse(job.getClusterId());

            if (cluster == null) {
                return CONFLICT("Cluster not found");
            }
        }

        List<JobAtPointDetails> jobsAtPoints = jobAtPointRepository.findJobAtPointDetails(jobId);

        List<SubJobDetails> subJobDetailsList = new ArrayList<>();

        boolean timeoutJob = false;
        boolean inServing = false;
        boolean completed = false;
        int downPayment = 0;

        List<String> pointNames = new ArrayList<>();

        String jobStatus = "Pending";

        if (!jobsAtPoints.isEmpty()) {
            for (JobAtPointDetails jobAtPoint : jobsAtPoints) {

                if (!pointNames.contains(jobAtPoint.getPointName())) {
                    pointNames.add(jobAtPoint.getPointName());
                }

                if (job.getStatus() == JobStatus.TRANSFER) {
                    jobStatus = "Transferred";
                } else if (jobAtPoint.getStatus() == JobStatus.IN_SERVICE) {
                    inServing = true;
                } else if (jobAtPoint.getStatus() == JobStatus.PENDING && completed) {
                    timeoutJob = true;
                } else {
                    if (jobAtPoint.getStatus() == JobStatus.COMPLETED) {
                        completed = true;
                    }

                    if (jobAtPoint.getStatus() == JobStatus.TIMEOUT) {
                        timeoutJob = true;
                    }
                }

                downPayment = downPayment + jobAtPoint.getDownPayment();

                List<String> services = jobAtPoint.getServices() == null
                        ? Collections.emptyList()
                        : Arrays.stream(jobAtPoint.getServices().split(",\\s*"))
                                .toList();

                SubJobDetails.PointJobDetails pointJobDetails = SubJobDetails.PointJobDetails.builder()
                        .services(services)
                        .startTime(CommonMethods.timeFormat(jobAtPoint.getExpectedStartTime().toString()))
                        .endTime(CommonMethods.timeFormat(jobAtPoint.getExpectedEndTime().toString()))
                        .actualStartTime(jobAtPoint.getStartedTime() != null
                                ? CommonMethods.timeFormat(jobAtPoint.getStartedTime().toString())
                                : null)
                        .actualEndTime(jobAtPoint.getEndTime() != null
                                ? CommonMethods.timeFormat(jobAtPoint.getEndTime().toString())
                                : null)
                        .agent(jobAtPoint.getAgent() != null ? jobAtPoint.getAgent() : null)
                        .status(jobAtPoint.getStatus())
                        .build();

                SubJobDetails subJobDetail = SubJobDetails.builder()
                        .pointName(jobAtPoint.getPointName())
                        .pointJobDetails(pointJobDetails)
                        .build();

                subJobDetailsList.add(subJobDetail);

            }
        }

        if (inServing) {
            jobStatus = "Serving";
        } else if (timeoutJob) {
            jobStatus = "Timeout";
        } else {
            if (completed) {
                jobStatus = "Completed";
            }
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        JobDetails jobDetails = JobDetails.builder()
                .id(job.getId())
                .customer(customer.getCustomer())
                .customerName(customer.getName())
                .pointName(pointNames)
                .customerEmail(null)
                .customerPhone(customer.getPhone())
                .serviceName(cluster != null ? cluster.getName() : "Custom Service")
                .centerName(job.getServiceCenter().getName())
                .status(jobStatus)
                .paidAmount((double) downPayment)
                .serviceFee(job.getServiceCenter().getServiceProvider().getServiceFee())
                .createdAt(job.getCreatedDate() + " at " + job.getCreatedTime().format(timeFormatter))
                .appointmentMethod(userServiceHelper.jobType(job.getJobType()))
                .appointmentDate(job.getAppointmentDate().toString())
                .appointmentTime(
                        job.getAppointmentTime() != null ? CommonMethods.timeFormat(job.getAppointmentTime().toString())
                                : " ")
                .description(job.getDescription())
                .plan(subJobDetailsList)
                .timeline(jobTrackRepository.getJobTrackByJobId(job.getId()))
                .verifiedJob(job.isPaymentVerified())
                .build();

        return DATA(jobDetails);
    }

}
