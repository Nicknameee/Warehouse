package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.ProductPhoto;
import io.store.ua.entity.Tag;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ProductDTO;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceIT extends AbstractIT {
    @Autowired
    private ProductService productService;

    private List<Tag> generateTags(int count) {
        return Stream.generate(() -> Tag.builder()
                        .name(GENERATOR.nextAlphanumeric(10))
                        .isActive(true)
                        .build())
                .limit(count)
                .toList();
    }

    private ProductDTO buildProductDTOWithTags(List<Long> tagIds) {
        return ProductDTO.builder()
                .code(GENERATOR.nextAlphanumeric(33))
                .title(GENERATOR.nextAlphanumeric(30))
                .description(GENERATOR.nextAlphanumeric(100))
                .price(BigInteger.valueOf(1000))
                .weight(BigInteger.valueOf(100))
                .length(BigInteger.valueOf(10))
                .width(BigInteger.valueOf(20))
                .height(BigInteger.valueOf(30))
                .tags(tagIds == null ? null : new HashSet<>(tagIds))
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

    private void saveProductPhoto(Long productId) {
        productPhotoRepository.save(ProductPhoto.builder()
                .productId(productId)
                .photoUrl(GENERATOR.nextAlphanumeric(333))
                .externalReference(GENERATOR.nextAlphanumeric(333))
                .build());
    }

    @Nested
    @DisplayName("save(productDTO: ProductDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: persists new Product without tags")
        void save_success_withoutTags() {
            ProductDTO productDTO = buildProductDTO();

            Product product = productService.save(productDTO);

            assertThat(product.getId())
                    .isNotNull();
            assertThat(product.getCode())
                    .isEqualTo(productDTO.getCode());
            assertThat(product.getTitle())
                    .isEqualTo(productDTO.getTitle());
            assertThat(product.getDescription())
                    .isEqualTo(productDTO.getDescription());
            assertThat(product.getPrice())
                    .isEqualByComparingTo(productDTO.getPrice());
            assertThat(product.getTags())
                    .isNullOrEmpty();
        }

        @Test
        @DisplayName("save_success: persists new Product with existing tags")
        void save_success_withTags() {
            List<Tag> tags = tagRepository.saveAll(generateTags(3));
            List<Long> tagIds = tags
                    .stream()
                    .map(Tag::getId)
                    .toList();

            ProductDTO productDTO = buildProductDTOWithTags(tagIds);

            Product product = productService.save(productDTO);

            assertThat(product.getId())
                    .isNotNull();
            assertThat(product.getTags())
                    .extracting(Tag::getId)
                    .containsExactlyInAnyOrderElementsOf(tagIds);
        }

        @Test
        @DisplayName("save_idempotent: returns existing Product when code already exists")
        void save_idempotent_existingCode() {
            ProductDTO productDTO = buildProductDTO();
            Product initialProduct = saveProduct(productDTO);

            Product product = productService.save(productDTO);

            assertThat(product.getId())
                    .isEqualTo(initialProduct.getId());
        }

        @Test
        @DisplayName("save_fail: throws NotFoundException when some tags do not exist")
        void save_fail_missingTags() {
            List<Tag> tags = tagRepository.saveAll(generateTags(10));
            List<Long> tagIds = new ArrayList<>(tags.stream().map(Tag::getId).toList());
            tagIds.add(Long.MAX_VALUE);

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
            ProductDTO productDTO = buildProductDTO();
            Product persisted = saveProduct(productDTO);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(productDTO.getCode())
                    .title(GENERATOR.nextAlphanumeric(100))
                    .description(GENERATOR.nextAlphanumeric(100))
                    .price(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .weight(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .length(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .width(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .height(BigInteger.valueOf(RandomUtils.secure().randomInt()))
                    .build();

            Product updatedProduct = productService.update(updateDTO);

            assertThat(updatedProduct.getId())
                    .isEqualTo(persisted.getId());
            assertThat(updatedProduct.getTitle())
                    .isEqualTo(updateDTO.getTitle());
            assertThat(updatedProduct.getDescription())
                    .isEqualTo(updateDTO.getDescription());
            assertThat(updatedProduct.getPrice())
                    .isEqualByComparingTo(updateDTO.getPrice());
            assertThat(updatedProduct.getWeight())
                    .isEqualTo(updateDTO.getWeight());
            assertThat(updatedProduct.getLength())
                    .isEqualTo(updateDTO.getLength());
            assertThat(updatedProduct.getWidth())
                    .isEqualTo(updateDTO.getWidth());
            assertThat(updatedProduct.getHeight())
                    .isEqualTo(updateDTO.getHeight());
        }

        @Test
        @DisplayName("update_success: replaces tags when provided")
        void update_success_replaceTags() {
            List<Tag> initialTags = tagRepository.saveAll(generateTags(2));
            List<Long> initialTagIds = initialTags.stream().map(Tag::getId).toList();

            ProductDTO productDTO = buildProductDTOWithTags(initialTagIds);

            Product product = saveProduct(productDTO);
            product.setTags(initialTags);

            productRepository.save(product);

            List<Tag> newTags = tagRepository.saveAll(generateTags(3));
            List<Long> newTagIds = newTags
                    .stream()
                    .map(Tag::getId)
                    .toList();

            productDTO = ProductDTO.builder()
                    .code(productDTO.getCode())
                    .tags(new HashSet<>(newTagIds))
                    .build();

            product = productService.update(productDTO);

            assertThat(product.getTags())
                    .extracting(Tag::getId)
                    .containsExactlyInAnyOrderElementsOf(newTagIds);
        }

        @Test
        @DisplayName("update_success: clears tags when empty set is provided")
        void update_success_clearTags() {
            List<Tag> initialTags = tagRepository.saveAll(generateTags(2));
            List<Long> initialTagIds = initialTags.stream().map(Tag::getId).toList();

            ProductDTO productDTO = buildProductDTOWithTags(initialTagIds);

            Product product = saveProduct(productDTO);
            product.setTags(initialTags);

            productRepository.save(product);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(productDTO.getCode())
                    .tags(new java.util.HashSet<>())
                    .build();

            product = productService.update(updateDTO);

            assertThat(product.getTags()).isEmpty();
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
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("update_fail: throws NotFoundException when provided tag IDs contain missing ones")
        void update_fail_missingTags() {
            ProductDTO productDTO = buildProductDTO();
            saveProduct(productDTO);

            List<Tag> existingTags = tagRepository.saveAll(generateTags(1));
            List<Long> mixedIds = new ArrayList<>(existingTags.stream()
                    .map(Tag::getId)
                    .toList());
            mixedIds.add(Long.MAX_VALUE);

            assertThatThrownBy(() -> productService.update(ProductDTO.builder()
                    .code(productDTO.getCode())
                    .tags(new HashSet<>(mixedIds))
                    .build()))
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

            List<Product> products = productService.findAll(pageSize, page);

            assertThat(products.size())
                    .isBetween(0, pageSize);
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

            List<Product> result = productService.findByTags(
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

            LocalDateTime from = LocalDateTime.now().minusDays(1);
            LocalDateTime to = LocalDateTime.now().plusDays(1);

            List<Product> result = productService.findBy(
                    "alpha",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000),
                    List.of(tagX.getId(), tagY.getId()),
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
            LocalDateTime from = LocalDateTime.now().plusDays(1);
            LocalDateTime to = LocalDateTime.now().minusDays(1);

            assertThatThrownBy(() -> productService.findBy(
                    "gamma",
                    null,
                    null,
                    null,
                    from,
                    to,
                    10,
                    1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("findBy_success: filters by all criteria and includes photos for matching products")
        void findBy_success_allFilters_withPhotos() {
            var tags = tagRepository.saveAll(generateTags(3));
            var tagX = tags.get(0);
            var tagY = tags.get(1);

            var titleToken = GENERATOR.nextAlphabetic(6);

            var dtoMatch = buildProductDTO();
            dtoMatch.setTitle(titleToken + "-" + GENERATOR.nextAlphanumeric(8));
            dtoMatch.setPrice(BigInteger.valueOf(RandomUtils.secure().randomLong(400, 800)));
            var productMatch = saveProduct(dtoMatch);
            productMatch.setTags(List.of(tagX, tagY));
            productRepository.save(productMatch);
            var photoUrlMatch = "https://cdn." + GENERATOR.nextAlphanumeric(6) + ".example/" +
                    GENERATOR.nextAlphanumeric(10) + ".png";
            var photoRefMatch = GENERATOR.nextAlphanumeric(18);
            productPhotoRepository.save(ProductPhoto.builder()
                    .productId(productMatch.getId())
                    .photoUrl(photoUrlMatch)
                    .externalReference(photoRefMatch)
                    .build());

            var dtoOther = buildProductDTO();
            dtoOther.setTitle(GENERATOR.nextAlphanumeric(12));
            dtoOther.setPrice(BigInteger.valueOf(RandomUtils.secure().randomLong(1_200, 2_000)));
            var productOther = saveProduct(dtoOther);
            productOther.setTags(List.of(tagX));
            productRepository.save(productOther);

            var createdFrom = LocalDateTime.now().minusDays(1);
            var createdTo = LocalDateTime.now().plusDays(1);
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
