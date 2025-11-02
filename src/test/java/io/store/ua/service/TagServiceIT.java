package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Tag;
import io.store.ua.exceptions.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TagServiceIT extends AbstractIT {
    @Autowired
    private TagService tagService;

    private List<Tag> generateTags(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(
                        ignore ->
                                Tag.builder()
                                        .name(RandomStringUtils.secure().nextAlphanumeric(10))
                                        .isActive(true)
                                        .build())
                .toList();
    }

    @Nested
    @DisplayName("save(tagName: string)")
    class SaveTests {
        @Test
        @DisplayName("save_success: creates a new Tag when absent")
        @Transactional
        void save_success() {
            String tagName = RandomStringUtils.secure().nextAlphanumeric(10);

            Tag tag = tagService.save(tagName);

            assertThat(tag.getId()).isNotNull();
            assertThat(tag.getName()).isEqualTo(tagName);
            assertThat(tag.getIsActive()).isTrue();
            assertThat(tagRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("save_idempotence: returns existing Tag when duplicate name is provided (idempotent)")
        @Transactional
        void save_idempotence() {
            String tagName = RandomStringUtils.secure().nextAlphanumeric(10);

            Tag tag = tagRepository.save(Tag.builder().name(tagName).isActive(true).build());

            Tag result = tagService.save(tagName);

            assertThat(result.getId()).isEqualTo(tag.getId());
            assertThat(result.getName()).isEqualTo(tag.getName());
            assertThat(tagRepository.count()).isEqualTo(1);
        }

        @ParameterizedTest(name = "save_fail_whenInvalidTagName: tagName=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void save_fail_whenInvalidTagName(String name) {
            assertThatThrownBy(() -> tagService.save(name))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("findAll(pageSize: int, page: int)")
    class FindAllTests {
        @ParameterizedTest(name = "findAll_success: returns a paged slice")
        @CsvSource({"1, 1", "3, 3", "5, 5"})
        @Transactional
        void findAll_success(int pageSize, int page) {
            var tags = tagRepository.saveAll(generateTags(pageSize * page));

            var result = tagService.findAll(pageSize, page);

            assertThat(result).hasSize(pageSize);
            assertThat(result)
                    .extracting(Tag::getName)
                    .containsExactlyInAnyOrderElementsOf(
                            tags.subList((page - 1) * pageSize, page * pageSize).stream()
                                    .map(Tag::getName)
                                    .toList());
        }

        @ParameterizedTest(name = "findAll_fail_whenPageSizeIsInvalid: pageSize={0} (must be >=1)")
        @ValueSource(ints = {0, -1, -5})
        void findAll_fail_whenPageSizeIsInvalid(int pageSize) {
            assertThatThrownBy(() -> tagService.findAll(pageSize, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "findAll_fail_whenPageIsInvalid: page={0} (must be >=1)")
        @ValueSource(ints = {0, -1, -10})
        void findAll_fail_whenPageIsInvalid(int page) {
            assertThatThrownBy(() -> tagService.findAll(10, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("toggleState(tagName: string)")
    class ToggleStateTests {
        @Test
        @DisplayName("toggleState_success: flips isActive and persists")
        @Transactional
        void toggleState_success() {
            String tagName = RandomStringUtils.secure().nextAlphanumeric(10);
            tagRepository.save(Tag.builder().name(tagName).isActive(true).build());

            Tag toggled = tagService.toggleState(tagName);

            assertThat(toggled.getIsActive()).isFalse();
            Tag reloaded = tagRepository.findByName(tagName).orElseThrow();
            assertThat(reloaded.getIsActive()).isFalse();
        }

        @Test
        @DisplayName(
                "toggleState_fail_whenTagWasNotFound: throws NotFoundException when tag is not found by name")
        void toggleState_fail_whenTagWasNotFound() {
            assertThatThrownBy(() -> tagService.toggleState("missing"))
                    .isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest(name = "toggleState_fail_whenTagNameIsInvalid: tagName=''{0}'' is invalid")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void toggleState_fail_whenTagNameIsInvalid(String name) {
            assertThatThrownBy(() -> tagService.toggleState(name))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("emptyTags()")
    class EmptyTagsTests {
        @Test
        @DisplayName("emptyTags_success: clears all tags that are not attached to any existing product")
        void emptyTags_success() {
            tagRepository.saveAll(generateTags(1));
            tagService.emptyTags();
            assertEquals(0, tagRepository.count());
        }
    }

    @Nested
    @DisplayName("isAttached (computed)")
    class IsAttachedTests {
        @Test
        @DisplayName("isAttached reflects whether a tag has a product_tags link")
        @Transactional
        void isAttached_computed_fromJoinTable() {
            Tag attachedTag = tagRepository.save(Tag.builder()
                    .name(RandomStringUtils.secure().nextAlphanumeric(12))
                    .isActive(true)
                    .build());

            Tag detachedTag = tagRepository.save(Tag.builder()
                    .name(RandomStringUtils.secure().nextAlphanumeric(12))
                    .isActive(true)
                    .build());

            var product = productRepository.save(io.store.ua.entity.Product.builder()
                    .code(RandomStringUtils.secure().nextAlphanumeric(24))
                    .title(RandomStringUtils.secure().nextAlphabetic(10))
                    .description(RandomStringUtils.secure().nextAlphanumeric(40))
                    .price(BigInteger.valueOf(RandomUtils.secure().randomInt(10, 500)))
                    .weight(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                    .length(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                    .width(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                    .height(BigInteger.valueOf(RandomUtils.secure().randomInt(1, 100)))
                    .build());

            entityManager.createNativeQuery(
                            "INSERT INTO product_tags (product_id, tag_id) VALUES (:p, :t)")
                    .setParameter("p", product.getId())
                    .setParameter("t", attachedTag.getId())
                    .executeUpdate();

            entityManager.flush();
            entityManager.clear();

            Tag reloadedAttached = tagRepository.findById(attachedTag.getId()).orElseThrow();
            Tag reloadedDetached = tagRepository.findById(detachedTag.getId()).orElseThrow();

            assertThat(reloadedAttached.getLinks()).isNotEmpty();
            assertThat(reloadedDetached.getLinks()).isEmpty();
        }
    }
}
