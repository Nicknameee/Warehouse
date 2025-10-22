package io.store.ua.service.external;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import io.store.ua.exceptions.HealthCheckException;
import io.store.ua.models.api.external.response.CloudinaryImageUploadResponse;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CloudinaryAPIServiceTest {
    private static final int MAX_SIZE_MB = 5;
    private static final String ROOT_FOLDER = "product_photos";
    private static final String EXPLICIT_FOLDER = RandomStringUtils.secure().nextAlphanumeric(24);

    private CloudinaryAPIService service;
    private Uploader uploader;

    @BeforeEach
    void setUp() {
        service = new CloudinaryAPIService(
                RandomStringUtils.secure().nextAlphanumeric(10),
                RandomStringUtils.secure().nextAlphanumeric(30),
                RandomStringUtils.secure().nextAlphanumeric(10),
                MAX_SIZE_MB,
                ROOT_FOLDER
        );

        uploader = Mockito.mock(Uploader.class);
        Cloudinary cloudinary = Mockito.mock(Cloudinary.class);
        when(cloudinary.uploader()).thenReturn(uploader);

        Field cloudinaryField = ReflectionUtils.findField(CloudinaryAPIService.class, CloudinaryAPIService.Fields.cloudinary);
        assertThat(cloudinaryField).isNotNull();
        cloudinaryField.setAccessible(true);
        ReflectionUtils.setField(cloudinaryField, service, cloudinary);
        cloudinaryField.setAccessible(false);
    }

    private void setHealth(boolean healthy) {
        CloudinaryAPIService spy = spy(service);
        doReturn(healthy).when(spy).isHealthy();
        service = spy;
    }

    private MockMultipartFile image(String filename, int numBytes) {
        return new MockMultipartFile("file", filename, "image/png", new byte[Math.max(numBytes, 0)]);
    }

    @Nested
    @DisplayName("uploadImage(file: MultipartFile, folder: String)")
    class UploadImageWithFolderTests {
        @Test
        @DisplayName("uploadImage_success: uploads image to the explicit folder")
        void uploadImage_success_uploadsImageToExplicitFolder() throws Exception {
            setHealth(true);
            String publicId = RandomStringUtils.secure().nextAlphanumeric(12);

            when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                    CloudinaryAPIService.Constants.PUBLIC_ID, publicId,
                    CloudinaryAPIService.Constants.URL, "http://cdn.example.com/abc/img.png",
                    CloudinaryAPIService.Constants.SECURE_URL, "https://cdn.example.com/abc/img.png"
            ));

            CloudinaryImageUploadResponse result =
                    service.uploadImage(image("front.png", 1536), EXPLICIT_FOLDER).join();

            assertThat(result.getPublicId()).isEqualTo(publicId);
            verify(uploader).upload(any(byte[].class), argThat(m -> EXPLICIT_FOLDER.equals(m.get("folder"))));
        }

        @Test
        @DisplayName("uploadImage_success: uses default folder when provided folder is blank")
        void uploadImage_success_usesDefaultFolderWhenBlank() throws Exception {
            setHealth(true);

            when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                    CloudinaryAPIService.Constants.PUBLIC_ID, RandomStringUtils.secure().nextAlphanumeric(10),
                    CloudinaryAPIService.Constants.URL, "http://cdn.example.com/def/img.png",
                    CloudinaryAPIService.Constants.SECURE_URL, "https://cdn.example.com/def/img.png"
            ));

            service.uploadImage(image("photo.png", 2048), " ").join();

            verify(uploader).upload(any(byte[].class), argThat(m -> ROOT_FOLDER.equals(m.get("folder"))));
        }

        @Test
        @DisplayName("uploadImage_fail: wraps IOException from Cloudinary in ValidationException")
        void uploadImage_fail_whenCloudinaryUploadThrowsIOException_wrapsInValidationException() throws Exception {
            setHealth(true);
            when(uploader.upload(any(), anyMap())).thenThrow(new IOException("network"));

            CompletableFuture<CloudinaryImageUploadResponse> future =
                    service.uploadImage(image("big.png", 4096), EXPLICIT_FOLDER);

            assertThatThrownBy(future::join)
                    .hasCauseInstanceOf(ValidationException.class)
                    .hasRootCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("uploadImage_fail: throws ValidationException when file is empty")
        void uploadImage_fail_whenFileIsEmpty_throwsValidationException() {
            setHealth(true);

            assertThatThrownBy(() -> service.uploadImage(
                    new MockMultipartFile("file", "empty.png", "image/png", new byte[0]), EXPLICIT_FOLDER))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("uploadImage_fail: throws ValidationException when content type is not image/*")
        void uploadImage_fail_whenFileIsNotImage_throwsValidationException() {
            setHealth(true);

            assertThatThrownBy(() -> service.uploadImage(
                    new MockMultipartFile("file", "notes.txt", "text/plain",
                            RandomStringUtils.secure().nextAlphanumeric(128).getBytes(StandardCharsets.UTF_8)),
                    EXPLICIT_FOLDER))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("uploadImage_fail: throws ValidationException when file exceeds max size")
        void uploadImage_fail_whenFileExceedsLimit_throwsValidationException() {
            setHealth(true);

            assertThatThrownBy(() -> service.uploadImage(
                    image("huge.png", MAX_SIZE_MB * 1024 * 1024 + 1), EXPLICIT_FOLDER))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("uploadImage_fail: throws HealthCheckException when service is unhealthy")
        void uploadImage_fail_whenServiceUnhealthy_throwsHealthCheckException() {
            setHealth(false);

            assertThatThrownBy(() -> service.uploadImage(image("front.png", 512), EXPLICIT_FOLDER))
                    .isInstanceOf(HealthCheckException.class);
        }
    }

    @Nested
    @DisplayName("deleteImage(publicId: String)")
    class DeleteImageTests {
        @Test
        @DisplayName("deleteImage_success: returns true for 'ok' and 'not_found'")
        void deleteImage_success_returnsTrueOnOkAndNotFound() throws Exception {
            setHealth(true);
            when(uploader.destroy(eq("img_ok"), anyMap())).thenReturn(Map.of("result", "ok"));
            when(uploader.destroy(eq("img_missing"), anyMap())).thenReturn(Map.of("result", "not_found"));

            assertThat(service.deleteImage("img_ok").join()).isTrue();
            assertThat(service.deleteImage("img_missing").join()).isTrue();
        }

        @Test
        @DisplayName("deleteImage_fail: wraps IOException from Cloudinary in ValidationException")
        void deleteImage_fail_whenCloudinaryDestroyThrowsIOException_wrapsInValidationException() throws Exception {
            setHealth(true);
            when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("boom"));

            assertThatThrownBy(() -> service.deleteImage(RandomStringUtils.secure().nextAlphanumeric(20)).join())
                    .hasCauseInstanceOf(ValidationException.class)
                    .hasRootCauseInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("deleteImage_fail: throws HealthCheckException when service is unhealthy")
        void deleteImage_fail_whenServiceUnhealthy_throwsHealthCheckException() {
            setHealth(false);

            assertThatThrownBy(() -> service.deleteImage(RandomStringUtils.secure().nextAlphanumeric(16)))
                    .isInstanceOf(HealthCheckException.class);
        }
    }

    @Nested
    @DisplayName("uploadImage(file: MultipartFile)")
    class UploadImageOverloadTests {
        @Test
        @DisplayName("uploadImage_success: uses default folder and returns response")
        void uploadImage_success_usesDefaultFolderAndReturnsResponse() throws Exception {
            setHealth(true);

            String publicId = RandomStringUtils.secure().nextAlphanumeric(10);
            String secureUrl = "https://cdn.example.com/" + RandomStringUtils.secure().nextAlphanumeric(8) + "/img.png";
            String url = secureUrl.replace("https", "http");

            when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                    CloudinaryAPIService.Constants.PUBLIC_ID, publicId,
                    CloudinaryAPIService.Constants.SECURE_URL, secureUrl,
                    CloudinaryAPIService.Constants.URL, url
            ));

            CloudinaryImageUploadResponse image =
                    service.uploadImage(new MockMultipartFile(
                                    RandomStringUtils.secure().nextAlphanumeric(8),
                                    "avatar.png",
                                    "image/png",
                                    new byte[768]))
                            .join();

            assertThat(image.getPublicId()).isEqualTo(publicId);
            assertThat(image.getSecureUrl()).isEqualTo(secureUrl);
            assertThat(image.getUrl()).isEqualTo(url);
            verify(uploader).upload(any(byte[].class), argThat(m -> ROOT_FOLDER.equals(m.get("folder"))));
        }

        @Test
        @DisplayName("uploadImage_fail: throws ValidationException when file is null")
        void uploadImage_fail_whenFileIsNull_throwsValidationException() {
            setHealth(true);
            assertThatThrownBy(() -> service.uploadImage(null).join())
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("uploadImage_fail: throws HealthCheckException when service is unhealthy")
        void uploadImage_fail_whenUnhealthy_throwsHealthCheckException() {
            setHealth(false);

            assertThatThrownBy(() -> service.uploadImage(
                    new MockMultipartFile("x", "pic.png", "image/png", new byte[256])))
                    .isInstanceOf(HealthCheckException.class);
        }
    }

    @Nested
    @DisplayName("uploadAllImages(files: List<MultipartFile>, folder: String)")
    class UploadAllImagesWithFolderTests {
        @Test
        @DisplayName("uploadAllImages_success: uploads all images to the explicit folder")
        void uploadAllImages_success_uploadsAllToExplicitFolder() throws Exception {
            setHealth(true);

            String id1 = RandomStringUtils.secure().nextAlphanumeric(10);
            String id2 = RandomStringUtils.secure().nextAlphanumeric(10);
            String id3 = RandomStringUtils.secure().nextAlphanumeric(10);

            when(uploader.upload(any(), anyMap()))
                    .thenReturn(Map.of(
                            CloudinaryAPIService.Constants.PUBLIC_ID, id1,
                            CloudinaryAPIService.Constants.URL, "http://cdn.example.com/a.png",
                            CloudinaryAPIService.Constants.SECURE_URL, "https://cdn.example.com/a.png"))
                    .thenReturn(Map.of(
                            CloudinaryAPIService.Constants.PUBLIC_ID, id2,
                            CloudinaryAPIService.Constants.URL, "http://cdn.example.com/b.png",
                            CloudinaryAPIService.Constants.SECURE_URL, "https://cdn.example.com/b.png"))
                    .thenReturn(Map.of(
                            CloudinaryAPIService.Constants.PUBLIC_ID, id3,
                            CloudinaryAPIService.Constants.URL, "http://cdn.example.com/c.png",
                            CloudinaryAPIService.Constants.SECURE_URL, "https://cdn.example.com/c.png"));

            List<CloudinaryImageUploadResponse> responses =
                    service.uploadAllImages(List.of(
                            image("1.png", 800),
                            image("2.png", 900),
                            image("3.png", 750)), EXPLICIT_FOLDER).join();

            assertThat(responses)
                    .extracting(CloudinaryImageUploadResponse::getPublicId)
                    .containsExactlyInAnyOrder(id1, id2, id3);

            verify(uploader, times(3))
                    .upload(any(byte[].class), argThat(m -> EXPLICIT_FOLDER.equals(m.get("folder"))));
        }

        @Test
        @DisplayName("uploadAllImages_success: uses default folder when folder not provided")
        void uploadAllImages_success_usesDefaultFolderWhenNotProvided() throws Exception {
            setHealth(true);

            when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                    CloudinaryAPIService.Constants.PUBLIC_ID, RandomStringUtils.secure().nextAlphanumeric(10),
                    CloudinaryAPIService.Constants.URL, "http://cdn.example.com/one.png",
                    CloudinaryAPIService.Constants.SECURE_URL, "https://cdn.example.com/one.png"));

            List<CloudinaryImageUploadResponse> responses =
                    service.uploadAllImages(List.of(image("only.png", 700))).join();

            assertThat(responses).hasSize(1);
            verify(uploader).upload(any(byte[].class), argThat(m -> ROOT_FOLDER.equals(m.get("folder"))));
        }

        @Test
        @DisplayName("uploadAllImages_fail: throws HealthCheckException when service is unhealthy")
        void uploadAllImages_fail_whenUnhealthy_throwsHealthCheckException() {
            setHealth(false);

            assertThatThrownBy(() -> service.uploadAllImages(List.of(image("a.png", 512))))
                    .isInstanceOf(HealthCheckException.class);
        }
    }

    @Nested
    @DisplayName("deleteAllImages(ids: List<String>)")
    class DeleteAllImagesTests {
        @Test
        @DisplayName("deleteAllImages_success: returns per-id boolean results")
        void deleteAllImages_success_returnsPerItemBooleans() throws Exception {
            setHealth(true);

            when(uploader.destroy(eq("img_ok"), anyMap())).thenReturn(Map.of("result", "ok"));
            when(uploader.destroy(eq("img_nf"), anyMap())).thenReturn(Map.of("result", "not_found"));

            List<Boolean> results = service.deleteAllImages(List.of("img_ok", "img_nf")).join();

            assertThat(results).containsExactly(true, true);
        }

        @Test
        @DisplayName("deleteAllImages_fail: throws HealthCheckException when service is unhealthy")
        void deleteAllImages_fail_whenUnhealthy_throwsHealthCheckException() {
            setHealth(false);

            assertThatThrownBy(() -> service.deleteAllImages(List.of("a", "b")))
                    .isInstanceOf(HealthCheckException.class);
        }
    }

    @Nested
    @DisplayName("deleteAllImagesStateful(ids: List<String>)")
    class DeleteAllImagesStatefulTests {
        @Test
        @DisplayName("deleteAllImagesStateful_success: returns map of id → result")
        void deleteAllImagesStateful_success_returnsMapOfResults() throws Exception {
            setHealth(true);

            when(uploader.destroy(eq("img_x"), anyMap())).thenReturn(Map.of("result", "ok"));
            when(uploader.destroy(eq("img_y"), anyMap())).thenReturn(Map.of("result", "not_found"));

            Map<String, Boolean> resultMap = service.deleteAllImagesStateful(List.of("img_x", "img_y")).join();

            assertThat(resultMap).containsEntry("img_x", true).containsEntry("img_y", true);
        }

        @Test
        @DisplayName("deleteAllImagesStateful_fail: throws HealthCheckException when service is unhealthy")
        void deleteAllImagesStateful_fail_whenUnhealthy_throwsHealthCheckException() {
            setHealth(false);

            assertThatThrownBy(() -> service.deleteAllImagesStateful(List.of("id1", "id2")))
                    .isInstanceOf(HealthCheckException.class);
        }
    }
}
