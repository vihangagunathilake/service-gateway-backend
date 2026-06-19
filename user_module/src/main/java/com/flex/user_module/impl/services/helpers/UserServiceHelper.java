package com.flex.user_module.impl.services.helpers;

import com.flex.common_module.security.impls.entities.ExpiredToken;
import com.flex.common_module.security.impls.repositories.ExpiredTokenRepository;
import com.flex.user_module.api.http.requests.EmployeeRegister;
import com.flex.user_module.impl.entities.UserLogin;
import com.flex.user_module.impl.repositories.UserLoginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/15/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class UserServiceHelper {

    private final UserLoginRepository userLoginRepository;
    private final ExpiredTokenRepository expiredTokenRepository;

    @Value("${app.image.url}")
    private String imageUrl;

    @Value("${app.storage.path}")
    private String mainImageStorage;

    @Value("${user.profile}")
    private String userProfile;

    @Value("${user.cover}")
    private String userCover;

    //close all non-logout login.
    public void logoutFromPreviousLogins(Integer userId) {

        List<UserLogin> login = userLoginRepository.getAllLogin(userId);

        if (login.size() > 0) {
            log.info("user {}", userId , "{} has previous login");

            List<UserLogin> prevLoginFixes = login.stream().peek(
                    l -> {
                        l.setLogoutTime(new Date());
                        l.setLogout(true);
                        //this is doing if prevent to access for logout tokens
                        expiredTokenRepository.save(
                                ExpiredToken.builder()
                                        .id(l.getToken())
                                        .userId(userId)
                                        .build()
                        );
                    }
            ).collect(Collectors.toList());

            userLoginRepository.saveAll(prevLoginFixes);
        }
    }

    public String employeeRegisterValidation(EmployeeRegister e) {
        if (e.getFName() == null || e.getFName().isEmpty()) {
            return "First name should not empty";
        }

        if (e.getLName() == null || e.getLName().isEmpty()) {
            return "Last name should not empty";
        }

        if (e.getContact() == null || e.getContact().isEmpty()) {
            return "Contact number should not empty";
        }

        if (e.getNic() == null || e.getNic().isEmpty()) {
            return "NIC should not empty";
        }

        if (e.getProviderId() == null || e.getProviderId().isEmpty()) {
            return "Provider ID should not empty";
        }

        return "success";
    }

    public String saveUserProfileImage(MultipartFile file, Integer userId) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path imageFolder = Paths.get(mainImageStorage + userProfile);

            if (!Files.exists(imageFolder)) {
                Files.createDirectories(imageFolder);
            }

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());

            Files.deleteIfExists(Paths.get(imageFolder + userId.toString() +
                    "." + extension));

            Files.copy(file.getInputStream(), imageFolder.resolve(userId
                    + "." + extension), StandardCopyOption.REPLACE_EXISTING);

            String profileImageUrl = imageUrl + userProfile;

            return setImageUrl(profileImageUrl, userId, extension);
        }
        return null;
    }

    public String saveUserCoverImage(MultipartFile file, Integer userId) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path imageFolder = Paths.get(mainImageStorage + userCover);

            if (!Files.exists(imageFolder)) {
                Files.createDirectories(imageFolder);
            }

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());

            Files.deleteIfExists(Paths.get(imageFolder + userId.toString() +
                    "." + extension));

            Files.copy(file.getInputStream(), imageFolder.resolve(userId
                    + "." + extension), StandardCopyOption.REPLACE_EXISTING);

            String profileImageUrl = imageUrl + userCover;

            return setImageUrl(profileImageUrl, userId, extension);
        }
        return null;
    }

    private String setImageUrl(String imageUrl, Integer id, String imageExtension) {
        return imageUrl + id + "." + imageExtension;
    }
}
