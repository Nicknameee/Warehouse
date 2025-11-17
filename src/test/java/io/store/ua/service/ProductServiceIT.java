package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.ProductPhoto;
import io.store.ua.entity.Tag;
import io.store.ua.enums.Currency;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ProductDTO;
import io.store.ua.utility.CodeGenerator;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
                .code(CodeGenerator.StockCodeGenerator.generate())
                .title(GENERATOR.nextAlphanumeric(30))
                .description(GENERATOR.nextAlphanumeric(100))
                .price(BigInteger.valueOf(1_000))
                .currency("UAH")
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
                .code(CodeGenerator.StockCodeGenerator.generate())
                .title(productDTO.getTitle())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .currency(productDTO.getCurrency())
                .weight(productDTO.getWeight())
                .length(productDTO.getLength())
                .width(productDTO.getWidth())
                .height(productDTO.getHeight())
                .build());
    }

    @Nested
    @DisplayName("findBy(...)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success: filters by title part, price range, created range and all tagIds (AND)")
        @Transactional
        void findBy_success_allFilters() {
            List<Tag> tags = tagRepository.saveAll(generateTags(3));
            Tag tagX = tags.get(0);
            Tag tagY = tags.get(1);

            ProductDTO productDTO = buildProductDTO();
            productDTO.setTitle(GENERATOR.nextAlphanumeric(10));
            productDTO.setPrice(BigInteger.valueOf(500));
            productDTO.setCurrency(Currency.USD.name());

            Product product = saveProduct(productDTO);
            product.setTags(new ArrayList<>(List.of(tagX, tagY)));
            productRepository.save(product);

            LocalDateTime from = LocalDateTime.now().minusDays(1);
            LocalDateTime to = LocalDateTime.now().plusDays(1);

            List<Product> result = productService.findBy(productDTO.getTitle().substring(0, 5),
                    product.getCode().substring(3, 9),
                    BigInteger.valueOf(100),
                    BigInteger.valueOf(1_000),
                    product.getCurrency(),
                    List.of(tagX.getId(), tagY.getId()),
                    from,
                    to,
                    10,
                    1);

            assertThat(result).extracting(Product::getId)
                    .containsExactly(product.getId());
        }

        @Test
        @DisplayName("findBy_fail: throws ValidationException when 'to' is before 'from'")
        void findBy_fail_toBeforeFrom() {
            LocalDateTime from = LocalDateTime.now().plusDays(1);
            LocalDateTime to = LocalDateTime.now().minusDays(1);

            assertThatThrownBy(() -> productService.findBy("gamma",
                    null,
                    null,
                    null,
                    null,
                    null,
                    from,
                    to,
                    10,
                    1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("A 'to' can't be before 'from'");
        }

        @Test
        @DisplayName("findBy_success: filters by all criteria and includes photos for matching products")
        void findBy_success_allFilters_withPhotos() {
            List<Tag> tags = tagRepository.saveAll(generateTags(3));
            Tag tag = tags.get(0);
            Tag otherTag = tags.get(1);

            String titleToken = GENERATOR.nextAlphabetic(6);

            ProductDTO productDTO = buildProductDTO();
            productDTO.setTitle(titleToken + "-" + GENERATOR.nextAlphanumeric(8));
            productDTO.setPrice(BigInteger.valueOf(RandomUtils.secure().randomLong(400, 800)));

            Product product = saveProduct(productDTO);
            product.setTags(new ArrayList<>(List.of(tag, otherTag)));
            product = productRepository.save(product);

            String photoUrlMatch = "https://cdn." + GENERATOR.nextAlphanumeric(6) + ".example/" +
                    GENERATOR.nextAlphanumeric(10) + ".png";
            String photoRefMatch = GENERATOR.nextAlphanumeric(18);

            productPhotoRepository.save(ProductPhoto.builder()
                    .productId(product.getId())
                    .photoUrl(photoUrlMatch)
                    .externalReference(photoRefMatch)
                    .build());

            LocalDateTime createdFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime createdTo = LocalDateTime.now().plusDays(1);
            BigInteger minPrice = BigInteger.valueOf(100);
            BigInteger maxPrice = BigInteger.valueOf(1_000);

            List<Product> result = productService.findBy(titleToken.toLowerCase(),
                    product.getCode().substring(3, 9),
                    minPrice,
                    maxPrice,
                    null,
                    List.of(tag.getId(), otherTag.getId()),
                    createdFrom,
                    createdTo,
                    10,
                    1
            );

            assertThat(result).extracting(Product::getId)
                    .containsExactly(product.getId());

            Product fetch = result.getFirst();

            assertThat(fetch.getPhotos())
                    .isNotEmpty();
            assertThat(fetch.getPhotos())
                    .extracting(ProductPhoto::getExternalReference)
                    .contains(photoRefMatch);
            assertThat(fetch.getPhotos())
                    .extracting(ProductPhoto::getPhotoUrl)
                    .contains(photoUrlMatch);
        }

        @Test
        @DisplayName("findBy_fail: throws ValidationException when minPrice > maxPrice")
        void findBy_fail_minGreaterThanMax() {
            assertThatThrownBy(() -> productService.findBy(null,
                    null,
                    BigInteger.valueOf(200),
                    BigInteger.valueOf(100),
                    null,
                    null,
                    null,
                    null,
                    10,
                    1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Minimum price can't be greater than max price");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("findBy_fail: throws ConstraintViolationException when pageSize invalid")
        void findBy_fail_whenPageSizeInvalid(int pageSize) {
            assertThatThrownBy(() -> productService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    pageSize,
                    1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("findBy_fail: throws ConstraintViolationException when page invalid")
        void findBy_fail_whenPageInvalid(int page) {
            assertThatThrownBy(() -> productService.findBy(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    10,
                    page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("save(productDTO: ProductDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: persists new Product without tags")
        void save_success_withoutTags() {
            ProductDTO productDTO = buildProductDTO();

            Product product = productService.save(productDTO);

            assertThat(product.getId()).isNotNull();
            assertThat(product.getCode()).isNotNull();
            assertThat(product.getTitle()).isEqualTo(productDTO.getTitle());
            assertThat(product.getDescription()).isEqualTo(productDTO.getDescription());
            assertThat(product.getPrice()).isEqualByComparingTo(productDTO.getPrice());
            assertThat(product.getCurrency()).isEqualTo(productDTO.getCurrency());
            assertThat(product.getTags()).isNullOrEmpty();
        }

        @Test
        @DisplayName("save_success: persists new Product with existing tags")
        void save_success_withTags() {
            List<Tag> tags = tagRepository.saveAll(generateTags(3));
            List<Long> tagIds = tags.stream().map(Tag::getId).toList();

            ProductDTO productDTO = buildProductDTOWithTags(tagIds);

            Product product = productService.save(productDTO);

            assertThat(product.getId()).isNotNull();
            assertThat(product.getTags())
                    .extracting(Tag::getId)
                    .containsExactlyInAnyOrderElementsOf(tagIds);
        }

        @Test
        @DisplayName("save_fail: throws NotFoundException when some tags do not exist")
        void save_fail_missingTags() {
            List<Tag> tags = tagRepository.saveAll(generateTags(10));
            List<Long> tagIds = new ArrayList<>(tags.stream()
                    .map(Tag::getId)
                    .toList());
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
            Product product = saveProduct(productDTO);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(product.getCode())
                    .title(GENERATOR.nextAlphanumeric(100))
                    .description(GENERATOR.nextAlphanumeric(100))
                    .price(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 10_000)))
                    .currency(Currency.USD.name())
                    .weight(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 10_000)))
                    .length(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 10_000)))
                    .width(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 10_000)))
                    .height(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 10_000)))
                    .build();

            Product updatedProduct = productService.update(updateDTO);

            assertThat(updatedProduct.getId())
                    .isEqualTo(product.getId());
            assertThat(updatedProduct.getTitle())
                    .isEqualTo(updateDTO.getTitle());
            assertThat(updatedProduct.getDescription())
                    .isEqualTo(updateDTO.getDescription());
            assertThat(updatedProduct.getPrice())
                    .isEqualByComparingTo(updateDTO.getPrice());
            assertThat(updatedProduct.getCurrency())
                    .isEqualTo(updateDTO.getCurrency());
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
            List<Tag> initialTags = tagRepository.saveAll(generateTags(3));
            List<Long> initialTagIds = initialTags.stream()
                    .map(Tag::getId)
                    .toList();

            ProductDTO productDTO = buildProductDTOWithTags(initialTagIds);

            Product product = saveProduct(productDTO);
            product.setTags(initialTags);
            productRepository.save(product);

            List<Tag> newTags = tagRepository.saveAll(generateTags(3));
            List<Long> newTagIds = newTags.stream()
                    .map(Tag::getId)
                    .toList();

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(product.getCode())
                    .tags(new HashSet<>(newTagIds))
                    .build();

            Product updated = productService.update(updateDTO);

            assertThat(updated.getTags())
                    .extracting(Tag::getId)
                    .containsExactlyInAnyOrderElementsOf(newTagIds);
        }

        @Test
        @DisplayName("update_success: clears tags when empty set is provided")
        void update_success_clearTags() {
            List<Tag> initialTags = tagRepository.saveAll(generateTags(3));
            List<Long> initialTagIds = initialTags.stream()
                    .map(Tag::getId)
                    .toList();

            ProductDTO productDTO = buildProductDTOWithTags(initialTagIds);

            Product product = saveProduct(productDTO);
            product.setTags(initialTags);
            productRepository.save(product);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(product.getCode())
                    .tags(new HashSet<>())
                    .build();

            Product updated = productService.update(updateDTO);

            assertThat(updated.getTags()).isEmpty();
        }

        @Test
        @DisplayName("update_fail: throws NotFoundException when product code does not exist")
        void update_fail_notFoundByCode() {
            ProductDTO productDTO = ProductDTO.builder()
                    .code("UNKNOWN_CODE")
                    .title(GENERATOR.nextAlphanumeric(10))
                    .description(GENERATOR.nextAlphanumeric(10))
                    .price(BigInteger.valueOf(100))
                    .currency(Currency.USD.name())
                    .weight(BigInteger.ONE)
                    .length(BigInteger.ONE)
                    .width(BigInteger.ONE)
                    .height(BigInteger.ONE)
                    .build();

            assertThatThrownBy(() -> productService.update(productDTO))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("update_fail: throws NotFoundException when provided tag IDs contain missing ones")
        void update_fail_missingTags() {
            ProductDTO productDTO = buildProductDTO();
            saveProduct(productDTO);

            List<Tag> existingTags = tagRepository.saveAll(generateTags(1));
            List<Long> mixedIds = new ArrayList<>(existingTags.stream().map(Tag::getId).toList());
            mixedIds.add(Long.MAX_VALUE);

            ProductDTO updateDTO = ProductDTO.builder()
                    .code(productDTO.getCode())
                    .tags(new HashSet<>(mixedIds))
                    .build();

            assertThatThrownBy(() -> productService.update(updateDTO))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
