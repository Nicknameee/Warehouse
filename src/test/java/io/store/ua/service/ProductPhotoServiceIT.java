package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.ProductPhoto;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.api.external.response.CloudinaryImageUploadResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductPhotoServiceIT extends AbstractIT {
    @Autowired
    private ProductPhotoService productPhotoService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = generateProduct();
    }

    private MockMultipartFile generateMockFile(String name, int size) {
        return new MockMultipartFile(
                GENERATOR.nextAlphanumeric(10),
                name,
                "image/png",
                new byte[Math.max(size, 0)]
        );
    }

    private MockMultipartFile generateMockTextFile(String name, String content) {
        return new MockMultipartFile(
                GENERATOR.nextAlphanumeric(10),
                name,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private CloudinaryImageUploadResponse generateCloudinaryImageUploadResponse(String publicId, String secureUrl, String url) {
        return CloudinaryImageUploadResponse.builder()
                .publicId(publicId)
                .secureUrl(secureUrl)
                .url(url)
                .build();
    }

    @Nested
    @DisplayName("saveAll(productId: Long, photos: List<MultipartFile>)")
    class SaveAllTests {
        @Test
        @DisplayName("saveAll_success: persists all uploaded photos with secure URL when present")
        void saveAll_success() {
            String publicFront = GENERATOR.nextAlphanumeric(12);
            String publicBack = GENERATOR.nextAlphanumeric(12);
            String secureFront = "https://cdn.example.com/images/" + GENERATOR.nextAlphanumeric(6) + "/front.png";
            String secureBack = "https://cdn.example.com/images/" + GENERATOR.nextAlphanumeric(6) + "/back.png";
            String httpFront = secureFront.replace("https", "http");
            String httpBack = secureBack.replace("https", "http");

            List<MultipartFile> files = List.of(generateMockFile("front.png", 1500), generateMockFile("back.png", 2000));

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(CompletableFuture.completedFuture(List.of(
                            generateCloudinaryImageUploadResponse(publicFront, secureFront, httpFront),
                            generateCloudinaryImageUploadResponse(publicBack, secureBack, httpBack))));

            List<ProductPhoto> productPhotos = productPhotoService.saveAll(product.getId(), files);

            verify(cloudinaryAPIService).uploadAllImages(files);
            assertThat(productPhotos).hasSize(2);
            assertThat(productPhotoRepository.count()).isEqualTo(2);

            assertThat(productPhotos).extracting(ProductPhoto::getProductId)
                    .containsOnly(product.getId());
            assertThat(productPhotos).extracting(ProductPhoto::getExternalReference)
                    .containsExactlyInAnyOrder(publicFront, publicBack);
            assertThat(productPhotos).extracting(ProductPhoto::getPhotoUrl)
                    .containsExactlyInAnyOrder(secureFront, secureBack);
        }

        @Test
        @DisplayName("saveAll_success_fallbackToHttp_whenSecureUrlIsAbsent: falls back to http URL when secure URL is absent")
        void saveAll_success_fallbackToHttp_whenSecureUrlIsAbsent() {
            String publicId = GENERATOR.nextAlphanumeric(14);
            String httpUrl = "http://cdn.example.com/images/" + GENERATOR.nextAlphanumeric(6) + "/only.png";

            List<MultipartFile> files = List.of(generateMockFile("only.png", 800));

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(CompletableFuture.completedFuture(List.of(
                            generateCloudinaryImageUploadResponse(publicId, null, httpUrl))));

            List<ProductPhoto> productPhotos = productPhotoService.saveAll(product.getId(), files);

            assertThat(productPhotos).hasSize(1);
            ProductPhoto photo = productPhotos.getFirst();
            assertThat(photo.getProductId()).isEqualTo(product.getId());
            assertThat(photo.getExternalReference()).isEqualTo(publicId);
            assertThat(photo.getPhotoUrl()).isEqualTo(httpUrl);
        }

        @Test
        @DisplayName("saveAll_success: accepts any multipart file type because API service validates type")
        void saveAll_success_anyMultipartTypeAllowedHere() {
            String publicId = GENERATOR.nextAlphanumeric(10);
            String secureUrl = "https://cdn.example.com/" + GENERATOR.nextAlphanumeric(5) + "/file.png";
            String url = secureUrl.replace("https", "http");

            List<MultipartFile> files = List.of(generateMockTextFile("not-image.txt", "payload"));

            when(cloudinaryAPIService.uploadAllImages(anyList())).thenReturn(CompletableFuture
                    .completedFuture(List.of(generateCloudinaryImageUploadResponse(publicId, secureUrl, url))));

            List<ProductPhoto> productPhotos = productPhotoService.saveAll(product.getId(), files);

            assertThat(productPhotos).hasSize(1);
            assertThat(productPhotos.getFirst().getExternalReference()).isEqualTo(publicId);
            assertThat(productPhotos.getFirst().getPhotoUrl()).isEqualTo(secureUrl);
        }

        @Test
        @DisplayName("saveAll_fail_whenPhotosListIsEmpty_throwsConstraintViolationException: throws ConstraintViolationException when photos list is empty")
        void saveAll_fail_whenPhotosListIsEmpty_throwsConstraintViolationException() {
            assertThatThrownBy(() -> productPhotoService.saveAll(product.getId(), List.of()))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "saveAll_fail_whenProductIdInvalid_throwsConstraintViolationException: productId={0}")
        @ValueSource(longs = {0L, -1L, -10L})
        @DisplayName("saveAll_fail_whenProductIdInvalid_throwsConstraintViolationException: throws ConstraintViolationException for invalid productId")
        void saveAll_fail_whenProductIdInvalid_throwsConstraintViolationException(Long invalidProductId) {
            List<MultipartFile> files = List.of(generateMockFile("a.png", 500));

            assertThatThrownBy(() -> productPhotoService.saveAll(invalidProductId, files))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "saveAll_fail_whenProductIdIsNull_throwsConstraintViolationException")
        @NullSource
        @DisplayName("saveAll_fail_whenProductIdIsNull_throwsConstraintViolationException: throws ConstraintViolationException when productId is null")
        void saveAll_fail_whenProductIdIsNull_throwsConstraintViolationException(Long nullId) {
            List<MultipartFile> files = List.of(generateMockFile("a.png", 500));

            assertThatThrownBy(() -> productPhotoService.saveAll(nullId, files))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("saveAll_fail_whenPhotoIsNull_throwsConstraintViolationException: throws ConstraintViolationException when a photo item is null")
        void saveAll_fail_whenPhotoIsNull_throwsConstraintViolationException() {
            List<MultipartFile> files = new ArrayList<>(List.of(generateMockFile("ok.png", 300),
                    generateMockFile("ok2.png", 400)));
            files.add(null);

            assertThatThrownBy(() -> productPhotoService.saveAll(product.getId(), files))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("saveAll_fail_whenProductDoesNotExist_throwsNotFoundException: throws NotFoundException when product does not exist")
        void saveAll_fail_whenProductDoesNotExist_throwsNotFoundException() {
            List<MultipartFile> files = List.of(generateMockFile("file.png", 1024));

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(CompletableFuture.completedFuture(List.of(
                            generateCloudinaryImageUploadResponse(
                                    GENERATOR.nextAlphanumeric(10),
                                    "https://cdn.example.com/test.png",
                                    "http://cdn.example.com/test.png"))));

            assertThatThrownBy(() -> productPhotoService.saveAll(Long.MAX_VALUE, files))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeAll(photoIds)")
    class RemoveAllTests {
        @Test
        @DisplayName("removeAll_success_deletesExistingPhotos_andCallsCloudinary")
        void removeAll_success_deletesExistingPhotos_andCallsCloudinary() {
            ProductPhoto photoOne = productPhotoRepository.save(ProductPhoto.builder()
                    .productId(product.getId())
                    .photoUrl("https://cdn/" + GENERATOR.nextAlphanumeric(10) + "/1.png")
                    .externalReference("pub_" + GENERATOR.nextAlphanumeric(10))
                    .build());

            ProductPhoto photoTwo = productPhotoRepository.save(ProductPhoto.builder()
                    .productId(product.getId())
                    .photoUrl("https://cdn/" + GENERATOR.nextAlphanumeric(6) + "/3.png")
                    .externalReference("pub_" + GENERATOR.nextAlphanumeric(10))
                    .build());

            long initialCount = productPhotoRepository.count();

            productPhotoService.removeAll(List.of(photoOne.getId(), photoTwo.getId()));

            verify(cloudinaryAPIService)
                    .deleteAllImages(List.of(photoOne.getExternalReference(), photoTwo.getExternalReference()));

            assertThat(productPhotoRepository.count())
                    .isEqualTo(initialCount - 2);

            assertThat(productPhotoRepository.findAllById(List.of(photoOne.getId(), photoTwo.getId())))
                    .isEmpty();
        }

        @Test
        @DisplayName("removeAll_success_ignoresNonExistingIds_deletesOnlyFound")
        void removeAll_success_ignoresNonExistingIds_deletesOnlyFound() {
            ProductPhoto photoExisting = productPhotoRepository.save(ProductPhoto.builder()
                    .productId(product.getId())
                    .photoUrl("https://cdn/" + GENERATOR.nextAlphanumeric(10) + "/only.png")
                    .externalReference("pub_" + GENERATOR.nextAlphanumeric(10))
                    .build());

            productPhotoService.removeAll(List.of(photoExisting.getId(), 99999999L));

            verify(cloudinaryAPIService)
                    .deleteAllImages(List.of(photoExisting.getExternalReference()));

            assertThat(productPhotoRepository.existsById(photoExisting.getId()))
                    .isFalse();
        }

        @Test
        @DisplayName("removeAll_fail_null_throwsConstraintViolation")
        void removeAll_fail_null_throwsConstraintViolation() {
            assertThatThrownBy(() -> productPhotoService.removeAll(null))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("removeAll_fail_empty_throwsConstraintViolation")
        void removeAll_fail_empty_throwsConstraintViolation() {
            assertThatThrownBy(() -> productPhotoService.removeAll(List.of()))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("removeAll_fail_containsNullId_throwsConstraintViolation")
        void removeAll_fail_containsNullId_throwsConstraintViolation() {
            var arg = new ArrayList<>(List.of(1L, 3L));
            arg.add(null);

            assertThatThrownBy(() -> productPhotoService.removeAll(arg))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -5L})
        @DisplayName("removeAll_fail_invalidId_throwsConstraintViolation")
        void removeAll_fail_invalidId_throwsConstraintViolation(Long photoId) {
            assertThatThrownBy(() -> productPhotoService.removeAll(List.of(photoId)))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
