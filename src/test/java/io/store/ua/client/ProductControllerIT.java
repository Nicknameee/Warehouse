package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.ProductPhoto;
import io.store.ua.entity.Tag;
import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.models.dto.ProductDTO;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIT extends AbstractIT {
    private static final DateTimeFormatter DMY_HMS = DateTimeFormatter.ofPattern("dd-MM-yyyy'At'HH:mm:ss");

    private static final String MANAGER = "manager";
    private HttpHeaders ownerAuthenticationHeaders;
    private HttpHeaders managerAuthenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        ownerAuthenticationHeaders = generateAuthenticationHeaders();
        managerAuthenticationHeaders = generateAuthenticationHeaders(MANAGER, MANAGER);
    }

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .username(MANAGER)
                .password(passwordEncoder.encode(MANAGER))
                .email("%s@example.com".formatted(MANAGER))
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .timezone("UTC")
                .build());
    }

    private List<Tag> generateTags(int count) {
        return tagRepository.saveAll(Stream.generate(() ->
                        Tag.builder()
                                .name(GENERATOR.nextAlphanumeric(10))
                                .isActive(true)
                                .build())
                .limit(count)
                .toList()
        );
    }

    private ProductDTO generateProductDTO(List<Long> tagIds) {
        return ProductDTO.builder()
                .code(GENERATOR.nextAlphanumeric(33))
                .title(GENERATOR.nextAlphanumeric(30))
                .description(GENERATOR.nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomLong(100, 5_000)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomLong(10, 500)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomLong(1, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomLong(1, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomLong(1, 100)))
                .tags(tagIds == null ? null : new HashSet<>(tagIds))
                .build();
    }

    private ProductDTO generateProductDTO() {
        return generateProductDTO(null);
    }

    private Product generateProduct(ProductDTO productDTO) {
        Product product = Product.builder()
                .code(productDTO.getCode())
                .title(productDTO.getTitle())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .weight(productDTO.getWeight())
                .length(productDTO.getLength())
                .width(productDTO.getWidth())
                .height(productDTO.getHeight())
                .build();

        return productRepository.save(product);
    }

    private void generateProductPhoto(Long productId) {
        productPhotoRepository.save(ProductPhoto.builder()
                .productId(productId)
                .photoUrl("https://cdn.example/%s%s".formatted(GENERATOR.nextAlphanumeric(33), ".png"))
                .externalReference(GENERATOR.nextAlphanumeric(24))
                .build());
    }

    private HttpHeaders generateHeaders(HttpHeaders headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(headers);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        return httpHeaders;
    }

    @Nested
    @DisplayName("GET /api/v1/products/findAll")
    class FindAllTests {
        @Test
        @DisplayName("findAll_success_returnsPaged")
        void findAll_success_returnsPaged() {
            for (int i = 0; i < 3; i++) {
                generateProduct(generateProductDTO());
            }

            String firstPageUrl = UriComponentsBuilder.fromPath("/api/v1/products/findAll")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            String otherPageUrl = UriComponentsBuilder.fromPath("/api/v1/products/findAll")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Product>> firstPage = restClient.exchange(firstPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            ResponseEntity<List<Product>> otherPage = restClient.exchange(otherPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(managerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(firstPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(otherPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody())
                    .isNotNull()
                    .hasSize(2);
            assertThat(otherPage.getBody())
                    .isNotNull()
                    .isNotEmpty();
        }

        @Test
        @DisplayName("findAll_fail_invalidPagination_returns4xx")
        void findAll_fail_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/products/findAll")
                    .queryParam("pageSize", 0)
                    .queryParam("page", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/findBy/code")
    class FindByCodeTests {
        @Test
        @DisplayName("findByCode_success_returnsProduct")
        void findByCode_success_returnsProduct() {
            ProductDTO productDTO = generateProductDTO();
            Product product = generateProduct(productDTO);
            generateProductPhoto(product.getId());

            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy/code")
                    .queryParam("code", productDTO.getCode())
                    .build(true)
                    .toUriString();

            ResponseEntity<Product> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    Product.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getId())
                    .isEqualTo(product.getId());
        }

        @Test
        @DisplayName("findByCode_fail_missing_returns4xx")
        void findByCode_fail_missing_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy/code")
                    .queryParam("code", "missing-code")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/findWith/tags")
    class FindWithTagsTests {
        @Test
        @DisplayName("findWithTags_success_intersectionAny_returnsMatching")
        void findWithTags_success_intersectionAny_returnsMatching() {
            List<Tag> tags = generateTags(3);
            Tag tagA = tags.get(0);
            Tag tagB = tags.get(1);
            Tag tagC = tags.get(2);

            Product product = generateProduct(generateProductDTO());
            product.setTags(List.of(tagA));
            productRepository.save(product);

            Product productOther = generateProduct(generateProductDTO());
            productOther.setTags(List.of(tagB));
            productRepository.save(productOther);

            Product productAnother = generateProduct(generateProductDTO());
            productAnother.setTags(List.of(tagC));
            productRepository.save(productAnother);

            String url = UriComponentsBuilder.fromPath("/api/v1/products/findWith/tags")
                    .queryParam("tagId", tagA.getId())
                    .queryParam("tagId", tagB.getId())
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Product>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(Product::getId)
                    .contains(product.getId(), productOther.getId());
            assertThat(response.getBody())
                    .extracting(Product::getId)
                    .doesNotContain(productAnother.getId());
        }

        @Test
        @DisplayName("findWithTags_fail_empty_returns4xx")
        void findWithTags_fail_empty_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/products/findWith/tags")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/findBy")
    class FindByEndpointTests {
        @Test
        @DisplayName("findBy_success_allFilters_AND_tags")
        void findBy_success_allFilters_AND_tags() {
            List<Tag> tags = generateTags(3);
            Tag tagX = tags.get(0);
            Tag tagY = tags.get(1);

            ProductDTO productDTO = generateProductDTO();
            productDTO.setTitle(GENERATOR.nextAlphanumeric(30));
            productDTO.setPrice(BigInteger.valueOf(600));
            Product product = generateProduct(productDTO);
            product.setTags(List.of(tagX, tagY));
            productRepository.save(product);

            productDTO = generateProductDTO();
            productDTO.setTitle(GENERATOR.nextAlphanumeric(30));
            productDTO.setPrice(BigInteger.valueOf(1500));
            product = generateProduct(productDTO);
            product.setTags(List.of(tagX));
            productRepository.save(product);

            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy")
                    .queryParam("titlePart", "alpha")
                    .queryParam("minimumPrice", new BigDecimal("100"))
                    .queryParam("maximumPrice", new BigDecimal("1000"))
                    .queryParam("tagIds", tagX.getId() + "," + tagY.getId())
                    .queryParam("from", DMY_HMS.format(LocalDateTime.now().minusDays(1)))
                    .queryParam("to", DMY_HMS.format(LocalDateTime.now().plusDays(1)))
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Product>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(Product::getId)
                    .containsExactly(product.getId());
        }

        @Test
        @DisplayName("findBy_fail_minGreaterThanMax_returns4xx")
        void findBy_fail_minGreaterThanMax_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy")
                    .queryParam("minimumPrice", new BigDecimal("300"))
                    .queryParam("maximumPrice", new BigDecimal("100"))
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class SaveEndpointTests {
        @Test
        @DisplayName("save_success_createsProduct")
        void save_success_createsProduct() {
            ProductDTO productDTO = generateProductDTO();

            ResponseEntity<Product> response = restClient.exchange(
                    "/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(productRepository.findByCode(productDTO.getCode()).isPresent())
                    .isTrue();
        }

        @Test
        @DisplayName("save_idempotent_existingCode_returnsSame")
        void save_idempotent_existingCode_returnsSame() {
            ProductDTO productDTO = generateProductDTO();

            ResponseEntity<Product> first = restClient.exchange("/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class);

            ResponseEntity<Product> second = restClient.exchange("/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class);

            assertThat(first.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(second.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(first.getBody())
                    .isNotNull();
            assertThat(second.getBody())
                    .isNotNull();
            assertThat(first.getBody().getId())
                    .isEqualTo(second.getBody().getId());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products")
    class UpdateEndpointTests {
        @Test
        @DisplayName("update_success_updatesFieldsAndTags")
        void update_success_updatesFieldsAndTags() {
            ProductDTO productDTO = generateProductDTO();
            ResponseEntity<Product> response = restClient.exchange("/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class);
            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<Tag> tags = generateTags(2);
            ProductDTO updateDTO = ProductDTO.builder()
                    .code(productDTO.getCode())
                    .title(GENERATOR.nextAlphanumeric(20))
                    .description(GENERATOR.nextAlphanumeric(30))
                    .price(BigInteger.valueOf(777))
                    .weight(BigInteger.valueOf(77))
                    .length(BigInteger.valueOf(17))
                    .width(BigInteger.valueOf(27))
                    .height(BigInteger.valueOf(37))
                    .tags(new java.util.HashSet<>(tags.stream().map(Tag::getId).toList()))
                    .build();

            ResponseEntity<Product> updated = restClient.exchange("/api/v1/products",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class);

            assertThat(updated.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(updated.getBody())
                    .isNotNull();
            assertThat(updated.getBody().getTitle())
                    .isEqualTo(updateDTO.getTitle());
            assertThat(updated.getBody().getDescription())
                    .isEqualTo(updateDTO.getDescription());
            assertThat(updated.getBody().getPrice())
                    .isEqualByComparingTo(updateDTO.getPrice());
            assertThat(updated.getBody().getTags())
                    .extracting(Tag::getId)
                    .containsExactlyInAnyOrderElementsOf(tags.stream().map(Tag::getId).toList());
        }

        @Test
        @DisplayName("update_fail_unknownCode_returns4xx")
        void update_fail_unknownCode_returns4xx() {
            ProductDTO productDTO = ProductDTO.builder()
                    .code(GENERATOR.nextAlphanumeric(300))
                    .title(GENERATOR.nextAlphanumeric(300))
                    .build();

            ResponseEntity<String> response = restClient.exchange("/api/v1/products",
                    HttpMethod.PUT,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
