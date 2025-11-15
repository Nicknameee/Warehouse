package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagServiceIT extends AbstractIT {
    @Autowired
    private TagService tagService;

    private List<Tag> generateTags(int count) {
        return Stream.generate(() -> Tag.builder()
                        .name(RandomStringUtils.secure().nextAlphanumeric(10))
                        .isActive(true)
                        .build())
                .limit(count)
                .toList();
    }

    @Nested
    @DisplayName("save(tagName: string)")
    class SaveTests {
        @Test
        @DisplayName("save_success: creates a new Tag when absent")
        void save_success() {
            String tagName = RandomStringUtils.secure().nextAlphanumeric(10);

            long initialCount = tagRepository.count();

            Tag tag = tagService.save(tagName);

            assertThat(tag.getId()).isNotNull();
            assertThat(tag.getName()).isEqualTo(tagName);
            assertThat(tag.getIsActive()).isTrue();
            assertThat(tagRepository.count()).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("save_idempotence: returns existing Tag when duplicate name is provided (idempotent)")
        @Transactional
        void save_idempotence() {
            String tagName = RandomStringUtils.secure().nextAlphanumeric(10);

            long initialCount = tagRepository.count();

            Tag tag = tagRepository.save(Tag.builder()
                    .name(tagName)
                    .isActive(true)
                    .build());

            Tag result = tagService.save(tagName);

            assertThat(result.getId()).isEqualTo(tag.getId());
            assertThat(result.getName()).isEqualTo(tag.getName());
            assertThat(tagRepository.count()).isEqualTo(initialCount + 1);
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
                    .containsExactlyInAnyOrderElementsOf(tags.subList((page - 1) * pageSize, page * pageSize)
                            .stream()
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
    @DisplayName("update(tagId: long, tagName: string, isActive: boolean)")
    class UpdateTests {
        @Test
        @DisplayName("update_success_true: sets isActive to true")
        @Transactional
        void update_success_isActiveTrue() {
            String newName = RandomStringUtils.secure().nextAlphanumeric(10);
            var tag = tagRepository.save(Tag.builder().name(RandomStringUtils.secure().nextAlphanumeric(10)).isActive(false).build());

            Tag updated = tagService.update(tag.getId(), newName, true);

            assertThat(updated.getIsActive()).isTrue();
            assertThat(tagRepository.findById(tag.getId())).isPresent().get()
                    .extracting(Tag::getIsActive).isEqualTo(true);
            assertThat(updated.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("update_success_false: sets isActive to false")
        @Transactional
        void update_success_isActiveFalse() {
            var tag = tagRepository.save(Tag.builder().name(RandomStringUtils.secure().nextAlphanumeric(10)).isActive(true).build());

            Tag updated = tagService.update(tag.getId(), null, false);

            var fetchResult = tagRepository.findById(tag.getId());
            assertThat(updated.getIsActive()).isFalse();
            assertTrue(fetchResult.isPresent());
            assertThat(fetchResult.get())
                    .extracting(Tag::getIsActive).isEqualTo(false);
            assertThat(updated.getName()).isEqualTo(tag.getName());
        }

        @Test
        @DisplayName("update_fail_whenTagWasNotFound: throws NotFoundException")
        void update_fail_whenTagWasNotFound() {
            assertThatThrownBy(() -> tagService.update(Long.MAX_VALUE, null, true))
                    .isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest(name = "update_ignores_blankTagName: tagName=''{0}'' leaves name unchanged")
        @ValueSource(strings = {"", " ", "\t", "\n"})
        @Transactional
        void update_ignores_blankTagName(String badName) {
            var original = tagRepository.save(Tag.builder()
                    .name(RandomStringUtils.secure().nextAlphanumeric(10))
                    .isActive(true)
                    .build());

            Tag updated = tagService.update(original.getId(), badName, null);

            assertThat(updated.getId()).isEqualTo(original.getId());
            assertThat(updated.getName()).isEqualTo(original.getName());
            assertThat(updated.getIsActive()).isEqualTo(original.getIsActive());
        }

        @ParameterizedTest(name = "update_fail_whenTagIdIsInvalid: tagId={0}")
        @NullSource
        @ValueSource(longs = {0, -1})
        void update_fail_whenTagIdIsInvalid(Long tagId) {
            assertThatThrownBy(() -> tagService.update(tagId, null, null))
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        }
    }
    @Nested
    @DisplayName("findBy(name: String, isActive: Boolean, pageSize: int, page: int)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_nameOnly_filtersBySubstringIgnoringCase")
        @Transactional
        void findBy_success_nameOnly_filtersBySubstringIgnoringCase() {
            Tag first = tagRepository.save(Tag.builder()
                    .name("apple")
                    .isActive(true)
                    .build());
            Tag second = tagRepository.save(Tag.builder()
                    .name("Application")
                    .isActive(false)
                    .build());
            tagRepository.save(Tag.builder()
                    .name("banana")
                    .isActive(true)
                    .build());

            List<Tag> result = tagService.findBy("app", null, 10, 1);

            assertThat(result)
                    .extracting(Tag::getName)
                    .containsExactlyInAnyOrder(first.getName(), second.getName());
        }

        @Test
        @DisplayName("findBy_success_isActiveOnly_true")
        @Transactional
        void findBy_success_isActiveOnly_true() {
            tagRepository.save(Tag.builder()
                    .name("tag1")
                    .isActive(true)
                    .build());
            tagRepository.save(Tag.builder()
                    .name("tag2")
                    .isActive(true)
                    .build());
            tagRepository.save(Tag.builder()
                    .name("tag3")
                    .isActive(false)
                    .build());

            List<Tag> result = tagService.findBy(null, true, 10, 1);

            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(tag -> Boolean.TRUE.equals(tag.getIsActive()));
        }

        @Test
        @DisplayName("findBy_success_isActiveOnly_false")
        @Transactional
        void findBy_success_isActiveOnly_false() {
            tagRepository.save(Tag.builder()
                    .name("active1")
                    .isActive(true)
                    .build());
            Tag first = tagRepository.save(Tag.builder()
                    .name("inactive1")
                    .isActive(false)
                    .build());
            Tag second = tagRepository.save(Tag.builder()
                    .name("inactive2")
                    .isActive(false)
                    .build());

            List<Tag> result = tagService.findBy(null, false, 10, 1);

            assertThat(result)
                    .extracting(Tag::getName)
                    .containsExactlyInAnyOrder(first.getName(), second.getName());
            assertThat(result).allMatch(tag -> Boolean.FALSE.equals(tag.getIsActive()));
        }

        @Test
        @DisplayName("findBy_success_nameAndActive_combinedFilter")
        @Transactional
        void findBy_success_nameAndActive_combinedFilter() {
            Tag activeApple = tagRepository.save(Tag.builder()
                    .name("Apple")
                    .isActive(true)
                    .build());
            Tag inactiveApple = tagRepository.save(Tag.builder()
                    .name("apple-past")
                    .isActive(false)
                    .build());
            Tag activePineapple = tagRepository.save(Tag.builder()
                    .name("Pineapple")
                    .isActive(true)
                    .build());
            tagRepository.save(Tag.builder()
                    .name("banana")
                    .isActive(true)
                    .build());

            List<Tag> result = tagService.findBy("app", true, 10, 1);

            assertThat(result)
                    .extracting(Tag::getName)
                    .containsExactlyInAnyOrder(activeApple.getName(), activePineapple.getName());
            assertThat(result).allMatch(t -> Boolean.TRUE.equals(t.getIsActive()));
        }

        @Test
        @DisplayName("findBy_success_pagination_splitsResultsAcrossPages")
        @Transactional
        void findBy_success_pagination_splitsResultsAcrossPages() {
            Tag t1 = tagRepository.save(Tag.builder()
                    .name("tag1")
                    .isActive(true)
                    .build());
            Tag t2 = tagRepository.save(Tag.builder()
                    .name("tag2")
                    .isActive(true)
                    .build());
            Tag t3 = tagRepository.save(Tag.builder()
                    .name("tag3")
                    .isActive(true)
                    .build());
            Tag t4 = tagRepository.save(Tag.builder()
                    .name("tag4")
                    .isActive(true)
                    .build());

            List<Tag> firstPage = tagService.findBy(null, true, 3, 1);
            List<Tag> secondPage = tagService.findBy(null, true, 1, 4);

            assertThat(firstPage).hasSize(3);
            assertThat(secondPage).hasSize(1);
            assertThat(firstPage).doesNotContainAnyElementsOf(secondPage);
            assertThat(firstPage)
                    .extracting(Tag::getName)
                    .containsAnyOf(t1.getName(), t2.getName(), t3.getName(), t4.getName());
            assertThat(secondPage)
                    .extracting(Tag::getName)
                    .containsAnyOf(t1.getName(), t2.getName(), t3.getName(), t4.getName());
        }

        @ParameterizedTest(name = "findBy_fail_whenPageSizeIsInvalid: pageSize={0}")
        @ValueSource(ints = {0, -1})
        void findBy_fail_whenPageSizeIsInvalid(int pageSize) {
            assertThatThrownBy(() -> tagService.findBy(null, null, pageSize, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "findBy_fail_whenPageIsInvalid: page={0}")
        @ValueSource(ints = {0, -1})
        void findBy_fail_whenPageIsInvalid(int page) {
            assertThatThrownBy(() -> tagService.findBy(null, null, 10, page))
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
            tagService.clearUnusedTags();
            assertEquals(0, tagRepository.count());
        }
    }

    @Nested
    @DisplayName("isAttached()")
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

            var product = productRepository.save(Product.builder()
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
                            "INSERT INTO product_tags (product_id, tag_id) VALUES (:a, :b)")
                    .setParameter("a", product.getId())
                    .setParameter("b", attachedTag.getId())
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
