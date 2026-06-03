package com.flex.user_module.impl.services;

import com.flex.common_module.constants.Colors;
import com.flex.common_module.http.pagination.Pagination;
import com.flex.common_module.http.pagination.Sorting;
import com.flex.common_module.security.http.response.UserClaims;
import com.flex.common_module.security.utils.CryptoUtil;
import com.flex.common_module.security.utils.HashUtil;
import com.flex.common_module.security.utils.JwtUtil;
import com.flex.service_module.impl.entities.ServiceCenter;
import com.flex.service_module.impl.entities.ServiceProvider;
import com.flex.service_module.impl.repositories.ServiceCenterRepository;
import com.flex.service_module.impl.repositories.ServiceProviderRepository;
import com.flex.user_module.api.DTO.CenterUsers;
import com.flex.user_module.api.http.requests.*;
import com.flex.user_module.api.http.responses.HeaderData;
import com.flex.user_module.api.http.responses.LoginResponse;
import com.flex.user_module.api.http.responses.UserData;
import com.flex.user_module.api.http.responses.UserProfileData;
import com.flex.user_module.api.services.UserService;
import com.flex.user_module.cache.RoleCacheService;
import com.flex.user_module.events.UserCreatedEvent;
import com.flex.user_module.events.UserUpdatedEvent;
import com.flex.user_module.impl.entities.*;
import com.flex.user_module.impl.entities.RolePermissionAccess;
import com.flex.user_module.impl.repositories.*;
import com.flex.user_module.impl.services.helpers.UserServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
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

    private final ApplicationEventPublisher publisher;

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


    @Override
    public ResponseEntity<?> register(Register register, HttpServletRequest request) {
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
            return CONFLICT("Permissions not found");
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
                rolePermission ->
                        RolePermissionAccess.builder()
                                .all_permission(true)
                                .rolePermission(rolePermission)
                                .build()
        ).toList();

        // create user entity for admin
        User user = User.builder()
                .fName(register.getAdminFName())
                .lName(register.getAdminLName())
                .email(register.getAdminEmail())
                .role(admin)
                .serviceProvider(serviceProvider)
                .userType(ADMIN)
                .password(HashUtil.hash(register.getAdminPassword()))
                .build();

        UserDetails userDetails = UserDetails.builder()
                .nic(CryptoUtil.encrypt(register.getNic()))
                .user(user)
                .addedTime(new Date())
                .build();

        roleRepository.save(admin);
        rolePermissionRepository.saveAll(rolePermissions);
        rolePermissionAccessRepository.saveAll(rolePermissionAccesses);
        userRepository.save(user);
        userDetailsRepository.save(userDetails);

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
                .build();

        return DATA(response);
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

        return DATA(
                HeaderData.builder()
                        .userType(user.getUserType() == 0 ? "USER"
                                : user.getUserType() == 1 ? "ADMIN"
                                        : "CUSTOMER")
                        .email(user.getEmail())
                        .providerId(user.getServiceProvider().getProviderId())
                        .serviceCenter(user.getServiceProvider().getName())
                        .userName(user.getFName())
                        .image(null)
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
                && prevUserDetails.getUser().getUserType() == USER) {
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
    public ResponseEntity<?> addUser(AddUser addUser, HttpServletRequest request) {
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

        Role role = roleRepository.findByIdAndDeletedIsFalse(addUser.getRoleId());

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

        int userType;

        if (addUser.getUserType().equals("ADMIN")) {
            userType = 1;
        } else if (addUser.getUserType().equals("EMPLOYEE")) {
            userType = 2;
        } else {
            userType = 0;
        }

        ServiceCenter serviceCenter = null;

        if (addUser.getServiceCenterId() != null) {
            serviceCenter = serviceCenterRepository.findByIdAndDeletedIsFalse(addUser.getServiceCenterId());
        }

        User user = User.builder()
                .fName(addUser.getFirstName())
                .lName(addUser.getLastName())
                .email(addUser.getEmail())
                .password(HashUtil.hash(addUser.getPassword()))
                .role(role)
                .serviceCenter(serviceCenter)
                .serviceProvider(serviceProvider)
                .userType(userType)
                .build();

        // adding role notification is happening in notification_module
        // this is added to prevent circular dependency
//        publisher.publishEvent(
//                new UserCreatedEvent(
//                        user
//                )
//        );

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

        userRepository.save(user);
        userDetailsRepository.save(userDetails);
        userStatusRepository.save(userStatus);

        return SUCCESS("Registration Completed");
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

        int userType;

        if (updateUser.getUserType().equals("ADMIN")) {
            userType = 1;
        } else if (updateUser.getUserType().equals("EMPLOYEE")) {
            userType = 2;
        } else {
            userType = 0;
        }

        if (existingUser.getUserType() != userType) {
            existingUser.setUserType(userType);
        }

        if (updateUser.getRoleId() != null
                && !existingUser.getRole().getId().equals(updateUser.getRoleId())
                && roleRepository.existsByIdAndDeletedIsFalse(updateUser.getRoleId())) {
            existingUser.setRole(new Role(updateUser.getRoleId()));

//            publisher.publishEvent(
//                    new UserUpdatedEvent(
//                            existingUser
//                    )
//            );
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

        if (updateUser.getPassword() != null
                && !updateUser.getPassword().isEmpty()
                && !HashUtil.checkEncrypted(updateUser.getPassword(), existingUser.getPassword())) {
            existingUser.setPassword(HashUtil.hash(updateUser.getPassword()));
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

        int userType = Integer.parseInt(updateUser.getUserType());

        if (existingUser.getUserType() != userType) {
            existingUser.setUserType(userType);
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

        if (updateUser.getPassword() != null
                && !updateUser.getPassword().isEmpty()
                && !HashUtil.checkEncrypted(updateUser.getPassword(), existingUser.getPassword())) {
            existingUser.setPassword(HashUtil.hash(updateUser.getPassword()));
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
}
