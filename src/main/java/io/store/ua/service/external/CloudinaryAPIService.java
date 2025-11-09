package io.store.ua.service.external;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.response.CloudinaryImageUploadResponse;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@FieldNameConstants
@Validated
public class CloudinaryAPIService implements ExternalAPIService {
    private final Cloudinary cloudinary;
    private final Integer limitSizeMB;
    private final String rootFolder;

    public CloudinaryAPIService(@Value("${cloudinary.cloud}") String cloud,
                                @Value("${cloudinary.credentials.apiKey}") String apiKey,
                                @Value("${cloudinary.credentials.apiSecret}") String apiSecret,
                                @Value("${cloudinary.limit}") Integer limitSizeMB,
                                @Value("${cloudinary.folder}") String folder) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                Constants.CLOUD_NAME, cloud,
                Constants.API_KEY, apiKey,
                Constants.API_SECRET, apiSecret
        ));
        this.limitSizeMB = limitSizeMB;
        this.rootFolder = folder;
    }

    public CompletableFuture<CloudinaryImageUploadResponse> uploadImage(@NotNull(message = "File can't be null") MultipartFile file) {
        return uploadImage(file, null);
    }

    public CompletableFuture<CloudinaryImageUploadResponse> uploadImage(@NotNull(message = "File can't be null") MultipartFile file,
                                                                        @NotBlank(message = "Folder name can't be blank") String folder) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        validateUploadingFile(file);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<?, ?> result = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                Constants.FOLDER, StringUtils.defaultIfBlank(folder, rootFolder),
                                Constants.RESOURCE_TYPE, Constants.IMAGE,
                                Constants.UNIQUE_FILENAME, true,
                                Constants.OVERWRITE, false,
                                Constants.WEBHOOK_URL, ""
                        )
                );


                return CloudinaryImageUploadResponse.builder()
                        .publicId((String) result.get(Constants.PUBLIC_ID))
                        .secureUrl((String) result.get(Constants.SECURE_URL))
                        .url((String) result.get(Constants.URL))
                        .build();
            } catch (IOException e) {
                throw new ValidationException("Failed to upload image to Cloudinary", e);
            }
        });
    }

    public CompletableFuture<Boolean> deleteImage(@NotBlank(message = "ID of image can't be blank") String publicID) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<?, ?> response = cloudinary.uploader().destroy(
                        publicID,
                        ObjectUtils.asMap(
                                Constants.INVALIDATE, true,
                                Constants.RESOURCE_TYPE, Constants.IMAGE,
                                Constants.WEBHOOK_URL, ""
                        )
                );

                String status = (String) response.get("result");

                return List.of(HttpStatus.OK.name(), HttpStatus.NOT_FOUND.name().replaceAll("_", " "))
                        .contains(status.replaceAll("_", " ").toUpperCase());
            } catch (IOException e) {
                throw new ValidationException("Failed to delete image from Cloudinary");
            }
        });
    }

    public CompletableFuture<List<CloudinaryImageUploadResponse>> uploadAllImages(@NotEmpty(message = "At least one photo should be present")
                                                                                  List<@NotNull(message = "A photo can't be null") MultipartFile> files) {
        return uploadAllImages(files, rootFolder);
    }

    public CompletableFuture<List<CloudinaryImageUploadResponse>> uploadAllImages(@NotEmpty(message = "At least one photo should be present")
                                                                                  List<@NotNull(message = "A photo can't be null") MultipartFile> files,
                                                                                  @NotBlank(message = "Folder name can't be blank") String folder) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        files.forEach(this::validateUploadingFile);

        List<CompletableFuture<CloudinaryImageUploadResponse>> futures = files.stream()
                .map(file -> uploadImage(file, folder))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignore -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    public CompletableFuture<List<Boolean>> deleteAllImages(@NotEmpty(message = "List of photo's public IDs can't be empty")
                                                            List<@NotBlank(message = "A photo public ID can't be blank") String> publicIDs) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        List<CompletableFuture<Boolean>> futures = publicIDs.stream()
                .map(this::deleteImage)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignore -> futures.stream().map(CompletableFuture::join).toList());
    }

    public CompletableFuture<Map<String, Boolean>> deleteAllImagesStateful(@NotEmpty(message = "List of photo's public IDs can't be empty")
                                                                           List<@NotBlank(message = "A photo public ID can't be blank") String> publicIDs) {
        if (!isHealthy()) {
            throw new HealthCheckException();
        }

        List<CompletableFuture<Map.Entry<String, Boolean>>> futures = publicIDs.stream()
                .map(id -> deleteImage(id)
                        .thenApply(ok -> Map.entry(id, ok)))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignore -> futures.stream().map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private void validateUploadingFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File '%s' is empty".formatted(file != null ? file.getOriginalFilename() : ""));
        }

        long maxSize = limitSizeMB * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new ValidationException("File size %d exceeds maximum allowed Cloudinary %dMB".formatted(file.getSize(), (maxSize / 1024 / 1024)));
        }

        String contentType = file.getContentType();

        if (contentType != null && !contentType.startsWith("image/")) {
            throw new ValidationException("Only image uploads are allowed, received: %s".formatted(contentType));
        }
    }

    static class Constants {
        static final String PUBLIC_ID = "public_id";
        static final String SECURE_URL = "secure_url";
        static final String URL = "url";
        static final String OVERWRITE = "overwrite";
        static final String RESOURCE_TYPE = "resource_type";
        static final String FOLDER = "folder";
        static final String UNIQUE_FILENAME = "unique_filename";
        static final String INVALIDATE = "invalidate";
        static final String IMAGE = "image";
        static final String WEBHOOK_URL = "notification_url";
        static final String CLOUD_NAME = "cloud_name";
        static final String API_KEY = "api_key";
        static final String API_SECRET = "api_secret";
    }
}
