package com.flex.user_module.api.services;

import com.flex.common_module.http.pagination.Pagination;
import com.flex.user_module.api.http.requests.*;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
public interface UserService {

    ResponseEntity<?> register(Register register, HttpServletRequest request) throws MessagingException;

    ResponseEntity<?> login(Login login, HttpServletRequest request);

    ResponseEntity<?> newPassword(ChangePassword changePassword, HttpServletRequest request);

    ResponseEntity<?> forgetPassword(ChangePassword changePassword, HttpServletRequest request) throws MessagingException;

    ResponseEntity<?> logout(HttpServletRequest request);

    ResponseEntity<?> headerData(HttpServletRequest request);

    ResponseEntity<?> permissions(HttpServletRequest request);

    ResponseEntity<?> permissionsWithAccess(HttpServletRequest request);

    ResponseEntity<?> employeeRegister(EmployeeRegister employeeRegister, HttpServletRequest request);

    ResponseEntity<?> getAllUsers(Pagination pagination, HttpServletRequest request);

    ResponseEntity<?> employeeAssign(Integer id, HttpServletRequest request);

    ResponseEntity<?> employeeReject(Integer id, HttpServletRequest request);

    ResponseEntity<?> decryptString(DecryptValue value, HttpServletRequest request);

    ResponseEntity<?> addUser(AddUser addUser, HttpServletRequest request) throws MessagingException;

    ResponseEntity<?> uploadProfileImage(Integer userId, MultipartFile profile, HttpServletRequest request);

    ResponseEntity<?> uploadCoverImage(Integer userId, MultipartFile cover, HttpServletRequest request);

    ResponseEntity<?> getUser(Integer userId, HttpServletRequest request);

    ResponseEntity<?> userProfileData(HttpServletRequest request);

    ResponseEntity<?> userProfileDetails(HttpServletRequest request);

    ResponseEntity<?> updateUser(AddUser addUser, HttpServletRequest request);

    ResponseEntity<?> updateUserProfile(AddUser addUser, HttpServletRequest request);

    //todo create upload user image

    ResponseEntity<?> deleteUser(Integer id, HttpServletRequest request);

    ResponseEntity<?> assignEmployeesToCenters(EmployeeAssign employeeAssign, HttpServletRequest request);

    ResponseEntity<?> usersByCenter(Integer centerId, HttpServletRequest request);

    ResponseEntity<?> nonAssignedUsers(Integer centerId, HttpServletRequest request);

    ResponseEntity<?> removeUserFromCenter(Integer userId, HttpServletRequest request);

    //todo create a service for customer registration - google OAuth
}
