package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.ProductPhoto;
import io.store.ua.entity.Tag;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ProductDTO;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceIT extends AbstractIT {
    @Autowired
    private ProductService productService;

    private List<Tag> generateTags(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(ignore ->
                        Tag.builder()
                                .name(RandomStringUtils.secure().nextAlphanumeric(10))
                                .isActive(true)
                                .build())
                .toList();
    }

    private ProductDTO buildProductDTOWithTags(List<Long> tagIds) {
        return ProductDTO.builder()
                .code(RandomStringUtils.secure().nextAlphanumeric(33))
                .title(RandomStringUtils.secure().nextAlphanumeric(30))
                .description("Description-" + RandomStringUtils.secure().nextAlphanumeric(100))
                .price(BigInteger.valueOf(1000))
                .weight(BigInteger.valueOf(100))
                .length(BigInteger.valueOf(10))
                .width(BigInteger.valueOf(20))
                .height(BigInteger.valueOf(30))
                .tags(tagIds == null ? null : new java.util.HashSet<>(tagIds))
                .build();
    }

    private ProductDTO buildProductDTO() {
        return buildProductDTOWithTags(null);
    }

    private Product saveProduct(ProductDTO productDTO) {
        return productRepository.save(Product.builder()
                .code(productDTO.getCode())
                .title(productDTO.getTitle())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .weight(productDTO.getWeight())
                .length(productDTO.getLength())
                .width(productDTO.getWidth())
                .height(productDTO.getHeight())
                .build());
    }

    private ProductPhoto saveProductPhoto(Long productId) {
        return productPhotoRepository.save(ProductPhoto.builder()
                .productId(productId)
                .photoUrl(RandomStringUtils.secure().nextAlphanumeric(333))
                .externalReference(RandomStringUtils.secure().nextAlphanumeric(333))
                .build());
    }

    @Nested
    @DisplayName("save(productDTO: ProductDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: persists new Product without tags")
        void save_success_withoutTags() {
            ProductDTO productDTO = buildProductDTO();

            Product savedProduct = productService.save(productDTO);

            assertThat(savedProduct.getId()).isNotNull();
            assertThat(savedProduct.getCode()).isEqualTo(productDTO.getCode());
            assertThat(savedProduct.getTitle()).isEqualTo(productDTO.getTitle());
            assertThat(savedProduct.getDescription()).isEqualTo(productDTO.getDescription());
            assertThat(savedProduct.getPrice()).isEqualByComparingTo(productDTO.getPrice());
            assertThat(savedProduct.getTags()).isNullOrEmpty();
        }

        @Test
        @DisplayName("save_success: persists new Product with existing tags")
        void save_success_withTags() {
            List<Tag> persistedTags = tagRepository.saveAll(generateTags(3));
            List<Long> tagIds = persistedTags.stream().map(Tag::getId).toList();

            ProductDTO productDTO = buildProductDTOWithTags(tagIds);

            Product savedProduct = productService.save(productDTO);

            assertThat(savedProduct.getId()).isNotNull();
            assertThat(savedProduct.getTags()).extracting(Tag::getId)
                    .containsExactlyInAnyOrderElementsOf(tagIds);
        }

        @Test
        @DisplayName("save_idempotent: returns existing Product when code already exists")
        void save_idempotent_existingCode() {
            ProductDTO productDTO = buildProductDTO();
            Product alreadySaved = saveProduct(productDTO);

            Product result = productService.save(productDTO);

            assertThat(result.getId()).isEqualTo(alreadySaved.getId());
        }

        @Test
        @DisplayName("save_fail: throws NotFoundException when some tags do not exist")
        void save_fail_missingTags() {
            List<Tag> persistedTags = tagRepository.saveAll(generateTags(10));
            List<Long> tagIds = new java.util.ArrayList<>(persistedTags.stream().map(Tag::getId).toList());
            tagIds.add(999999L);

            ProductDTO productDTO = buildProductDTOWithTags(tagIds);

            assertThatThrownBy(() -> productService.save(productDTO))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update(productDTO: ProductDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success: updates all provided scalar fields")
        void update_success_scalars() {
            ProductDTO originalDTO = buildProductDTO();
            Product persisted = saveProduct(originalDTO);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(originalDTO.getCode())
                    .title(RandomStringUtils.secure().nextAlphanumeric(100))
                    .description(RandomStringUtils.secure().nextAlphanumeric(100))
                    .price(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .weight(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .length(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .width(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .height(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .build();

            Product updated = productService.update(updateDTO);

            assertThat(updated.getId()).isEqualTo(persisted.getId());
            assertThat(updated.getTitle()).isEqualTo(updateDTO.getTitle());
            assertThat(updated.getDescription()).isEqualTo(updateDTO.getDescription());
            assertThat(updated.getPrice()).isEqualByComparingTo(updateDTO.getPrice());
            assertThat(updated.getWeight()).isEqualTo(updateDTO.getWeight());
            assertThat(updated.getLength()).isEqualTo(updateDTO.getLength());
            assertThat(updated.getWidth()).isEqualTo(updateDTO.getWidth());
            assertThat(updated.getHeight()).isEqualTo(updateDTO.getHeight());
        }

        @Test
        @DisplayName("update_success: replaces tags when provided")
        void update_success_replaceTags() {
            List<Tag> initialTags = tagRepository.saveAll(generateTags(2));
            List<Long> initialTagIds = initialTags.stream().map(Tag::getId).toList();

            ProductDTO originalDTO = buildProductDTOWithTags(initialTagIds);
            Product persisted = saveProduct(originalDTO);
            persisted.setTags(initialTags);
            productRepository.save(persisted);

            List<Tag> newTags = tagRepository.saveAll(generateTags(3));
            List<Long> newTagIds = newTags.stream().map(Tag::getId).toList();

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(originalDTO.getCode())
                    .tags(new java.util.HashSet<>(newTagIds))
                    .build();

            Product updated = productService.update(updateDTO);

            assertThat(updated.getTags()).extracting(Tag::getId)
                    .containsExactlyInAnyOrderElementsOf(newTagIds);
        }

        @Test
        @DisplayName("update_success: clears tags when empty set is provided")
        void update_success_clearTags() {
            List<Tag> initialTags = tagRepository.saveAll(generateTags(2));
            List<Long> initialTagIds = initialTags.stream().map(Tag::getId).toList();

            ProductDTO originalDTO = buildProductDTOWithTags(initialTagIds);
            Product persisted = saveProduct(originalDTO);
            persisted.setTags(initialTags);
            productRepository.save(persisted);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(originalDTO.getCode())
                    .tags(new java.util.HashSet<>())
                    .build();

            Product updated = productService.update(updateDTO);

            assertThat(updated.getTags()).isEmpty();
        }

        @Test
        @DisplayName("update_fail: throws NotFoundException when product code does not exist")
        void update_fail_notFoundByCode() {
            ProductDTO updateDTO = ProductDTO.builder()
                    .code("UNKNOWN_CODE")
                    .title("X")
                    .description("Y")
                    .price(BigInteger.valueOf(100))
                    .weight(BigInteger.ONE)
                    .length(BigInteger.ONE)
                    .width(BigInteger.ONE)
                    .height(BigInteger.ONE)
                    .build();

            assertThatThrownBy(() -> productService.update(updateDTO))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Product with code");
        }

        @Test
        @DisplayName("update_fail: throws NotFoundException when provided tag IDs contain missing ones")
        void update_fail_missingTags() {
            ProductDTO baseDTO = buildProductDTO();
            saveProduct(baseDTO);

            List<Tag> existingTags = tagRepository.saveAll(generateTags(1));
            List<Long> mixedIds = new java.util.ArrayList<>(existingTags.stream().map(Tag::getId).toList());
            mixedIds.add(123456789L);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(baseDTO.getCode())
                    .tags(new java.util.HashSet<>(mixedIds))
                    .build();

            assertThatThrownBy(() -> productService.update(updateDTO))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll(pageSize: int, page: int)")
    class FindAllTests {
        @ParameterizedTest
        @CsvSource({"1,1", "2,1", "2,2"})
        @DisplayName("findAll_success: returns page slice using 1-based page index")
        void findAll_success(int pageSize, int page) {
            for (int i = 0; i < 5; i++) {
                ProductDTO productDTO = buildProductDTO();
                saveProduct(productDTO);
            }

            List<Product> result = productService.findAll(pageSize, page);

            assertThat(result.size()).isBetween(0, pageSize);
        }
    }

    @Nested
    @DisplayName("findWithTags(tagIds: List<Long>, pageSize: int, page: int)")
    class FindWithTagsTests {
        @Test
        @DisplayName("findWithTags_success: returns only products that have at least one of provided tags")
        void findWithTags_success() {
            List<Tag> tags = tagRepository.saveAll(generateTags(3));
            Tag tagA = tags.get(0);
            Tag tagB = tags.get(1);

            Product product1 = saveProduct(buildProductDTO());
            product1.setTags(List.of(tagA));
            productRepository.save(product1);

            Product product2 = saveProduct(buildProductDTO());
            product2.setTags(List.of(tagB));
            productRepository.save(product2);

            Product product3 = saveProduct(buildProductDTO());
            product3.setTags(List.of(tags.get(2)));
            productRepository.save(product3);

            List<Product> result = productService.findWithTags(
                    List.of(tagA.getId(), tagB.getId()),
                    10, 1
            );

            assertThat(result).extracting(Product::getId)
                    .contains(product1.getId(), product2.getId())
                    .doesNotContain(product3.getId());
        }
    }

    @Nested
    @DisplayName("findBy(...)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success: filters by title part, price range, created range and all tagIds (AND)")
        void findBy_success_allFilters() {
            List<Tag> tags = tagRepository.saveAll(generateTags(3));
            Tag tagX = tags.get(0);
            Tag tagY = tags.get(1);

            ProductDTO dtoA = buildProductDTO();
            dtoA.setTitle("Alpha Gadget");
            dtoA.setPrice(BigInteger.valueOf(500));
            Product productA = saveProduct(dtoA);
            productA.setTags(List.of(tagX, tagY));
            productRepository.save(productA);

            ProductDTO dtoB = buildProductDTO();
            dtoB.setTitle("Beta Tool");
            dtoB.setPrice(BigInteger.valueOf(1500));
            Product productB = saveProduct(dtoB);
            productB.setTags(List.of(tagX));
            productRepository.save(productB);

            ZonedDateTime from = ZonedDateTime.now().minusDays(1);
            ZonedDateTime to = ZonedDateTime.now().plusDays(1);

            List<Product> result = productService.findBy(
                    "alpha",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000),
                    java.util.List.of(tagX.getId(), tagY.getId()),
                    from,
                    to,
                    10,
                    1
            );

            assertThat(result).extracting(Product::getId)
                    .containsExactly(productA.getId());
        }

        @Test
        @DisplayName("findBy_success: swaps created range when from > to")
        void findBy_success_swapCreatedRange() {
            ProductDTO dto = buildProductDTO();
            dto.setTitle("Gamma");
            Product product = saveProduct(dto);

            ZonedDateTime from = ZonedDateTime.now().plusDays(1);
            ZonedDateTime to = ZonedDateTime.now().minusDays(1);

            List<Product> result = productService.findBy(
                    "gamma",
                    null,
                    null,
                    null,
                    from,
                    to,
                    10,
                    1
            );

            assertThat(result).extracting(Product::getId).contains(product.getId());
        }

        @Test
        @DisplayName("findBy_success: filters by all criteria and includes photos for matching products")
        void findBy_success_allFilters_withPhotos() {
            var tags = tagRepository.saveAll(generateTags(3));
            var tagX = tags.get(0);
            var tagY = tags.get(1);

            var titleToken = RandomStringUtils.secure().nextAlphabetic(6);

            var dtoMatch = buildProductDTO();
            dtoMatch.setTitle(titleToken + "-" + RandomStringUtils.secure().nextAlphanumeric(8));
            dtoMatch.setPrice(BigInteger.valueOf(RandomUtils.secure().randomLong(400, 800)));
            var productMatch = saveProduct(dtoMatch);
            productMatch.setTags(List.of(tagX, tagY));
            productRepository.save(productMatch);
            var photoUrlMatch = "https://cdn." + RandomStringUtils.secure().nextAlphanumeric(6) + ".example/" +
                    RandomStringUtils.secure().nextAlphanumeric(10) + ".png";
            var photoRefMatch = RandomStringUtils.secure().nextAlphanumeric(18);
            productPhotoRepository.save(ProductPhoto.builder()
                    .productId(productMatch.getId())
                    .photoUrl(photoUrlMatch)
                    .externalReference(photoRefMatch)
                    .build());

            var dtoOther = buildProductDTO();
            dtoOther.setTitle(RandomStringUtils.secure().nextAlphanumeric(12));
            dtoOther.setPrice(BigInteger.valueOf(RandomUtils.secure().randomLong(1_200, 2_000)));
            var productOther = saveProduct(dtoOther);
            productOther.setTags(List.of(tagX));
            productRepository.save(productOther);

            var createdFrom = ZonedDateTime.now().minusDays(1);
            var createdTo = ZonedDateTime.now().plusDays(1);
            var minPrice = BigDecimal.valueOf(100);
            var maxPrice = BigDecimal.valueOf(1_000);

            var result = productService.findBy(
                    titleToken.toLowerCase(),
                    minPrice,
                    maxPrice,
                    List.of(tagX.getId(), tagY.getId()),
                    createdFrom,
                    createdTo,
                    10,
                    1
            );

            assertThat(result).extracting(Product::getId)
                    .containsExactly(productMatch.getId());

            var returned = result.getFirst();
            assertThat(returned.getPhotos()).isNotEmpty();
            assertThat(returned.getPhotos())
                    .extracting(ProductPhoto::getExternalReference)
                    .contains(photoRefMatch);
            assertThat(returned.getPhotos())
                    .extracting(ProductPhoto::getPhotoUrl)
                    .contains(photoUrlMatch);
        }

        @Test
        @DisplayName("findBy_fail: throws ValidationException when minPrice > maxPrice")
        void findBy_fail_minGreaterThanMax() {
            assertThatThrownBy(() -> productService.findBy(
                    null,
                    BigDecimal.valueOf(200),
                    BigDecimal.valueOf(100),
                    null,
                    null,
                    null,
                    10,
                    1
            )).isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Minimum price can't be greater than max price");
        }
    }

    @Nested
    @DisplayName("findByCode(code: String)")
    class FindByCodeTests {
        @Test
        @DisplayName("findByCode_success: returns product by code")
        void findByCode_success() {
            ProductDTO productDTO = buildProductDTO();
            Product product = saveProduct(productDTO);
            saveProductPhoto(product.getId());

            Product found = productService.findByCode(productDTO.getCode());

            assertThat(found.getId()).isEqualTo(product.getId());
            assertThat(found.getPhotos()).isNotEmpty();
            assertThat(found.getPhotos()).extracting(ProductPhoto::getProductId).contains(product.getId());
        }

        @Test
        @DisplayName("findByCode_fail: throws NotFoundException when code is missing")
        void findByCode_fail() {
            assertThatThrownBy(() -> productService.findByCode("missing-code"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Product with code");
        }
    }
}
