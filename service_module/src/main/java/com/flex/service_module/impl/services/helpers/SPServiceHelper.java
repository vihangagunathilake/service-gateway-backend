package com.flex.service_module.impl.services.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/30/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class SPServiceHelper {

    @Value("${app.image.url}")
    private String imageUrl;

    @Value("${app.storage.path}")
    private String mainImageStorage;

    @Value("${provider.profile}")
    private String providerProfile;

    @Value("${provider.cover}")
    private String providerCover;

    public boolean isValidAndDifferent(String newVal, String oldVal) {
        return newVal != null &&
                !newVal.trim().isEmpty() &&
                !newVal.equals(oldVal);
    }

    public String saveProviderProfileImage(MultipartFile file, Integer userId) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path imageFolder = Paths.get(mainImageStorage + providerProfile);

            if (!Files.exists(imageFolder)) {
                Files.createDirectories(imageFolder);
            }

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());

            Files.deleteIfExists(Paths.get(imageFolder + userId.toString() +
                    "." + extension));

            Files.copy(file.getInputStream(), imageFolder.resolve(userId
                    + "." + extension), StandardCopyOption.REPLACE_EXISTING);

            String profileImageUrl = imageUrl + providerProfile;

            return setImageUrl(profileImageUrl, userId, extension);
        }
        return null;
    }

    public String saveProviderCoverImage(MultipartFile file, Integer userId) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path imageFolder = Paths.get(mainImageStorage + providerCover);

            if (!Files.exists(imageFolder)) {
                Files.createDirectories(imageFolder);
            }

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());

            Files.deleteIfExists(Paths.get(imageFolder + userId.toString() +
                    "." + extension));

            Files.copy(file.getInputStream(), imageFolder.resolve(userId
                    + "." + extension), StandardCopyOption.REPLACE_EXISTING);

            String profileImageUrl = imageUrl + providerCover;

            return setImageUrl(profileImageUrl, userId, extension);
        }
        return null;
    }

    private String setImageUrl(String imageUrl, Integer id, String imageExtension) {
        return imageUrl + id + "." + imageExtension;
    }
}
