package io.store.ua.controller;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.ProductPhoto;
import io.store.ua.models.api.external.response.CloudinaryImageUploadResponse;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class ProductPhotoControllerIT extends AbstractIT {
    private HttpHeaders ownerAuthenticationHeaders;
    private Product product;

    @BeforeAll
    void setupAuthentication() {
        ownerAuthenticationHeaders = generateAuthenticationHeaders();
    }

    @BeforeEach
    void setUp() {
        product = generateProduct();
    }

    private MockMultipartFile generateMockImage(String filename, int size) {
        return new MockMultipartFile("photo",
                filename,
                "image/png",
                new byte[Math.max(size, 0)]);
    }

    private MockMultipartFile generateMockText(String filename, String content) {
        return new MockMultipartFile("photo",
                filename,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private CloudinaryImageUploadResponse cloudinaryUpload(String publicId, String secureUrl, String url) {
        return CloudinaryImageUploadResponse.builder()
                .publicId(publicId)
                .secureUrl(secureUrl)
                .url(url)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/productPhotos/saveAll")
    class SaveAllTests {
        @Test
        @DisplayName("saveAll_success_multipleFiles")
        void saveAll_success_multipleFiles() {
            String publicA = GENERATOR.nextAlphanumeric(10);
            String publicB = GENERATOR.nextAlphanumeric(10);
            String secureA = "https://cdn.example/%s%s".formatted(GENERATOR.nextAlphanumeric(6), "/a.png");
            String secureB = "https://cdn.example/%s%s".formatted(GENERATOR.nextAlphanumeric(6), "/b.png");
            String httpA = secureA.replace("https", "http");
            String httpB = secureB.replace("https", "http");

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(CompletableFuture.completedFuture(List.of(
                            cloudinaryUpload(publicA, secureA, httpA),
                            cloudinaryUpload(publicB, secureB, httpB))));

            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("productId", product.getId().toString());
            multipartBody.add("photo", generateMockImage("front.png", 1024).getResource());
            multipartBody.add("photo", generateMockImage("back.png", 2048).getResource());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(ownerAuthenticationHeaders);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<List<ProductPhoto>> responseEntity = restClient.exchange(
                    "/api/v1/productPhotos/saveAll",
                    HttpMethod.POST,
                    new HttpEntity<>(multipartBody, httpHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .hasSize(2);
            assertThat(responseEntity.getBody())
                    .extracting(ProductPhoto::getProductId)
                    .containsOnly(product.getId());
            assertThat(responseEntity.getBody())
                    .extracting(ProductPhoto::getExternalReference)
                    .containsExactlyInAnyOrder(publicA, publicB);
            assertThat(responseEntity.getBody())
                    .extracting(ProductPhoto::getPhotoUrl)
                    .containsExactlyInAnyOrder(secureA, secureB);
        }

        @Test
        @DisplayName("saveAll_success_fallbackToHttpWhenNoSecure")
        void saveAll_success_fallbackToHttpWhenNoSecure() {
            String publicId = GENERATOR.nextAlphanumeric(12);
            String httpUrl = "http://cdn.example/" + GENERATOR.nextAlphanumeric(6) + "/only.png";

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                            java.util.List.of(cloudinaryUpload(publicId, null, httpUrl))
                    ));

            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("productId", product.getId().toString());
            multipartBody.add("photo", generateMockImage("only.png", 888).getResource());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(ownerAuthenticationHeaders);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<List<ProductPhoto>> responseEntity = restClient.exchange(
                    "/api/v1/productPhotos/saveAll",
                    HttpMethod.POST,
                    new HttpEntity<>(multipartBody, httpHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(responseEntity.getBody().getFirst().getExternalReference())
                    .isEqualTo(publicId);
            assertThat(responseEntity.getBody().getFirst().getPhotoUrl())
                    .isEqualTo(httpUrl);
        }

        @Test
        @DisplayName("saveAll_success_acceptsAnyMultipartType_serviceValidates")
        void saveAll_success_acceptsAnyMultipartType_serviceValidates() {
            String publicId = GENERATOR.nextAlphanumeric(10);
            String secureUrl = "https://cdn.example/" + GENERATOR.nextAlphanumeric(6) + "/f.png";
            String httpUrl = secureUrl.replace("https", "http");

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                            java.util.List.of(cloudinaryUpload(publicId, secureUrl, httpUrl))
                    ));

            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("productId", product.getId().toString());
            multipartBody.add("photo", generateMockText("not-image.txt", "payload").getResource());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(ownerAuthenticationHeaders);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<List<ProductPhoto>> responseEntity = restClient.exchange(
                    "/api/v1/productPhotos/saveAll",
                    HttpMethod.POST,
                    new HttpEntity<>(multipartBody, httpHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(responseEntity.getBody().getFirst().getExternalReference())
                    .isEqualTo(publicId);
            assertThat(responseEntity.getBody().getFirst().getPhotoUrl())
                    .isEqualTo(secureUrl);
        }

        @Test
        @DisplayName("saveAll_fail_emptyPhotos_returns4xx")
        void saveAll_fail_emptyPhotos_returns4xx() {
            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("productId", product.getId().toString());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(ownerAuthenticationHeaders);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<String> responseEntity = restClient.exchange("/api/v1/productPhotos/saveAll",
                    HttpMethod.POST,
                    new HttpEntity<>(multipartBody, httpHeaders),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("saveAll_fail_missingProduct_returns4xx")
        void saveAll_fail_missingProduct_returns4xx() {
            String secureUrl = "https://cdn.example/" + GENERATOR.nextAlphanumeric(5) + "/x.png";
            String httpUrl = secureUrl.replace("https", "http");
            String publicId = GENERATOR.nextAlphanumeric(10);

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                            java.util.List.of(cloudinaryUpload(publicId, secureUrl, httpUrl))
                    ));

            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("productId", String.valueOf(Long.MAX_VALUE));
            multipartBody.add("photo", generateMockImage("x.png", 256).getResource());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(ownerAuthenticationHeaders);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<String> responseEntity = restClient.exchange(
                    "/api/v1/productPhotos/saveAll",
                    HttpMethod.POST,
                    new HttpEntity<>(multipartBody, httpHeaders),
                    String.class
            );

            assertThat(responseEntity.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/productPhotos/removeAll")
    class RemoveAllTests {
        @Test
        @DisplayName("removeAll_success_deletesByIds")
        void removeAll_success_deletesByIds() {
            String publicA = GENERATOR.nextAlphanumeric(10);
            String publicB = GENERATOR.nextAlphanumeric(10);
            String secureA = "https://cdn.example/" + GENERATOR.nextAlphanumeric(6) + "/a.png";
            String secureB = "https://cdn.example/" + GENERATOR.nextAlphanumeric(6) + "/b.png";
            String httpA = secureA.replace("https", "http");
            String httpB = secureB.replace("https", "http");

            when(cloudinaryAPIService.uploadAllImages(anyList()))
                    .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                            java.util.List.of(
                                    cloudinaryUpload(publicA, secureA, httpA),
                                    cloudinaryUpload(publicB, secureB, httpB)
                            )
                    ));

            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("productId", product.getId().toString());
            multipartBody.add("photo", generateMockImage("a.png", 100).getResource());
            multipartBody.add("photo", generateMockImage("b.png", 200).getResource());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(ownerAuthenticationHeaders);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<List<ProductPhoto>> saveResponse = restClient.exchange(
                    "/api/v1/productPhotos/saveAll",
                    HttpMethod.POST,
                    new HttpEntity<>(multipartBody, httpHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<ProductPhoto> saved = saveResponse.getBody();
            assertThat(saved)
                    .isNotNull()
                    .hasSize(2);

            String url = org.springframework.web.util.UriComponentsBuilder.fromPath("/api/v1/productPhotos/removeAll")
                    .queryParam("photoId", saved.get(0).getId())
                    .queryParam("photoId", saved.get(1).getId())
                    .build(true)
                    .toUriString();

            ResponseEntity<Void> deleteResponse = restClient.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    Void.class
            );

            assertThat(deleteResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(productPhotoRepository.count())
                    .isEqualTo(0L);
        }
    }
}
