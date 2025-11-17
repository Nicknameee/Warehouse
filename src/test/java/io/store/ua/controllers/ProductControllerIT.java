package io.store.ua.controllers;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.Tag;
import io.store.ua.enums.Currency;
import io.store.ua.models.dto.ProductDTO;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIT extends AbstractIT {
    private static final DateTimeFormatter DMY_HMS = DateTimeFormatter.ofPattern("dd-MM-yyyy'At'HH:mm:ss");

    private HttpHeaders ownerAuthenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        ownerAuthenticationHeaders = generateAuthenticationHeaders();
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
                .currency(Currency.EUR.name())
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
                .currency(Currency.USD.name())
                .tags(productDTO.getTags() == null ? null : tagRepository.findAllById(productDTO.getTags()))
                .weight(productDTO.getWeight())
                .length(productDTO.getLength())
                .width(productDTO.getWidth())
                .height(productDTO.getHeight())
                .build();

        return productRepository.save(product);
    }

    private HttpHeaders generateHeaders(HttpHeaders headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(headers);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }

    @Nested
    @DisplayName("GET /api/v1/products/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_allFilters_applied")
        void findBy_success_allFilters_applied() {
            List<Tag> tags = generateTags(3);
            Tag tagX = tags.get(0);
            Tag tagY = tags.get(1);

            ProductDTO productDTO = generateProductDTO(Stream.of(tagX, tagY).map(Tag::getId).toList());
            productDTO.setTitle(GENERATOR.nextAlphanumeric(30));
            productDTO.setPrice(BigInteger.valueOf(600));
            Product product = generateProduct(productDTO);
            productRepository.save(product);

            ProductDTO otherDto = generateProductDTO(List.of(tagX.getId()));
            otherDto.setTitle(GENERATOR.nextAlphanumeric(9));
            otherDto.setPrice(BigInteger.valueOf(1500));
            Product otherProduct = generateProduct(otherDto);
            productRepository.save(otherProduct);

            String url = UriComponentsBuilder.fromPath("/api/v1/products/findBy")
                    .queryParam("titlePart", product.getTitle().substring(5, 15))
                    .queryParam("codePart", product.getCode().substring(0, 8))
                    .queryParam("minimumPrice", BigInteger.valueOf(100))
                    .queryParam("maximumPrice", BigInteger.valueOf(1000))
                    .queryParam("tagId", tagX.getId())
                    .queryParam("tagId", tagY.getId())
                    .queryParam("from", DMY_HMS.format(LocalDateTime.now().minusDays(1)))
                    .queryParam("to", DMY_HMS.format(LocalDateTime.now().plusDays(1)))
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Product>> response = restClient.exchange(
                    url,
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
            assertThat(productRepository.findByCode(response.getBody().getCode()).isPresent())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products")
    class UpdateEndpointTests {
        @Test
        @DisplayName("update_success_updatesFieldsAndTags")
        void update_success_updatesFieldsAndTags() {
            ProductDTO productDTO = generateProductDTO();
            ResponseEntity<Product> created = restClient.exchange(
                    "/api/v1/products",
                    HttpMethod.POST,
                    new HttpEntity<>(productDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Product.class
            );

            assertThat(created.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            List<Tag> tags = generateTags(2);
            ProductDTO updateDTO = ProductDTO.builder()
                    .code(created.getBody().getCode())
                    .title(GENERATOR.nextAlphanumeric(20))
                    .description(GENERATOR.nextAlphanumeric(30))
                    .price(BigInteger.valueOf(777))
                    .weight(BigInteger.valueOf(77))
                    .length(BigInteger.valueOf(17))
                    .width(BigInteger.valueOf(27))
                    .height(BigInteger.valueOf(37))
                    .tags(new java.util.HashSet<>(tags.stream().map(Tag::getId).toList()))
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
                    .containsExactlyInAnyOrderElementsOf(tags.stream().map(Tag::getId).toList());
        }
    }
}
