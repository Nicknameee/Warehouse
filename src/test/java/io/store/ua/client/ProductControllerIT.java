package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.ProductPhoto;
import io.store.ua.entity.Tag;
import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.models.dto.ProductDTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        return tagRepository.saveAll(
                Stream.generate(() -> Tag.builder()
                                .name(RandomStringUtils.secure().nextAlphanumeric(10))
                                .isActive(true)
                                .build())
                        .limit(count)
                        .toList()
        );
    }

    private ProductDTO generateProductDTO(List<Long> tagIds) {
        return ProductDTO.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(33))
                .title(RandomStringUtils.secure().nextAlphanumeric(30))
                .description("D-" + RandomStringUtils.secure().nextAlphanumeric(40))
                .price(BigInteger.valueOf(RandomUtils.secure().randomLong(100, 5_000)))
                .weight(BigInteger.valueOf(RandomUtils.secure().randomLong(10, 500)))
                .length(BigInteger.valueOf(RandomUtils.secure().randomLong(1, 100)))
                .width(BigInteger.valueOf(RandomUtils.secure().randomLong(1, 100)))
                .height(BigInteger.valueOf(RandomUtils.secure().randomLong(1, 100)))
                .tags(tagIds == null ? null : new java.util.HashSet<>(tagIds))
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
                .photoUrl("https://cdn.example/" + RandomStringUtils.secure().nextAlphanumeric(16) + ".png")
                .externalReference(RandomStringUtils.secure().nextAlphanumeric(24))
                .build());
    }

    private HttpHeaders generateHeaders(HttpHeaders base) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(base);
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

            String secondPageUrl = UriComponentsBuilder.fromPath("/api/v1/products/findAll")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Product>> firstPage = restClient.exchange(
                    firstPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            ResponseEntity<List<Product>> secondPage = restClient.exchange(
                    secondPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(managerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(firstPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(secondPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody())
                    .isNotNull()
                    .hasSize(2);
            assertThat(secondPage.getBody())
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

            ResponseEntity<String> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

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
            Product persisted = generateProduct(productDTO);
            generateProductPhoto(persisted.getId());

            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy/code")
                    .queryParam("code", productDTO.getCode())
                    .build(true)
                    .toUriString();

            ResponseEntity<Product> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    Product.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getId())
                    .isEqualTo(persisted.getId());
        }

        @Test
        @DisplayName("findByCode_fail_missing_returns4xx")
        void findByCode_fail_missing_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy/code")
                    .queryParam("code", "missing-code")
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

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

            Product product1 = generateProduct(generateProductDTO());
            product1.setTags(List.of(tagA));
            productRepository.save(product1);

            Product product2 = generateProduct(generateProductDTO());
            product2.setTags(List.of(tagB));
            productRepository.save(product2);

            Product product3 = generateProduct(generateProductDTO());
            product3.setTags(List.of(tagC));
            productRepository.save(product3);

            String url = UriComponentsBuilder.fromPath("/api/v1/products/findWith/tags")
                    .queryParam("tagId", tagA.getId())
                    .queryParam("tagId", tagB.getId())
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Product>> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(Product::getId)
                    .contains(product1.getId(), product2.getId());
            assertThat(response.getBody())
                    .extracting(Product::getId)
                    .doesNotContain(product3.getId());
        }

        @Test
        @DisplayName("findWithTags_fail_empty_returns4xx")
        void findWithTags_fail_empty_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/products/findWith/tags")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

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

            ProductDTO dtoA = generateProductDTO();
            dtoA.setTitle("Alpha Device");
            dtoA.setPrice(BigInteger.valueOf(600));
            Product productA = generateProduct(dtoA);
            productA.setTags(List.of(tagX, tagY));
            productRepository.save(productA);

            ProductDTO dtoB = generateProductDTO();
            dtoB.setTitle("Beta Gadget");
            dtoB.setPrice(BigInteger.valueOf(1500));
            Product productB = generateProduct(dtoB);
            productB.setTags(List.of(tagX));
            productRepository.save(productB);

            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy")
                    .queryParam("titlePart", "alpha")
                    .queryParam("minimumPrice", new BigDecimal("100"))
                    .queryParam("maximumPrice", new BigDecimal("1000"))
                    .queryParam("tagIds", tagX.getId() + "," + tagY.getId())
                    .queryParam("createdFromInclusive", DMY_HMS.format(LocalDateTime.now().minusDays(1)))
                    .queryParam("createdToInclusive", DMY_HMS.format(LocalDateTime.now().plusDays(1)))
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Product>> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(Product::getId)
                    .containsExactly(productA.getId());
        }

        @Test
        @DisplayName("findBy_fail_minGreaterThanMax_returns4xx")
        void findBy_fail_minGreaterThanMax_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy")
                    .queryParam("minimumPrice", new BigDecimal("200"))
                    .queryParam("maximumPrice", new BigDecimal("100"))
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    String.class
            );

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

            ResponseEntity<Product> first = restClient.exchange(
                    "/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class
            );

            ResponseEntity<Product> second = restClient.exchange(
                    "/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class
            );

            assertThat(first.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(second.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(first.getBody()).isNotNull();
            assertThat(second.getBody()).isNotNull();
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
            ProductDTO originalDTO = generateProductDTO();
            ResponseEntity<Product> created = restClient.exchange(
                    "/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(originalDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class
            );
            assertThat(created.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<Tag> newTags = generateTags(2);
            ProductDTO updateDTO = ProductDTO.builder()
                    .code(originalDTO.getCode())
                    .title(RandomStringUtils.secure().nextAlphanumeric(20))
                    .description(RandomStringUtils.secure().nextAlphanumeric(30))
                    .price(BigInteger.valueOf(777))
                    .weight(BigInteger.valueOf(77))
                    .length(BigInteger.valueOf(17))
                    .width(BigInteger.valueOf(27))
                    .height(BigInteger.valueOf(37))
                    .tags(new java.util.HashSet<>(newTags.stream().map(Tag::getId).toList()))
                    .build();

            ResponseEntity<Product> updated = restClient.exchange(
                    "/api/v1/products",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class
            );

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
                    .containsExactlyInAnyOrderElementsOf(newTags.stream().map(Tag::getId).toList());
        }

        @Test
        @DisplayName("update_fail_unknownCode_returns4xx")
        void update_fail_unknownCode_returns4xx() {
            ProductDTO updateDTO = ProductDTO.builder()
                    .code("UNKNOWN-" + RandomStringUtils.secure().nextAlphanumeric(8))
                    .title("X")
                    .build();

            ResponseEntity<String> response = restClient.exchange(
                    "/api/v1/products",
                    HttpMethod.PUT,
                    new HttpEntity<>(updateDTO, generateHeaders(ownerAuthenticationHeaders)),
                    String.class
            );

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
