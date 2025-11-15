package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemGroupDTO;
import io.store.ua.utility.CodeGenerator;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemGroupServiceIT extends AbstractIT {
    @Autowired
    private StockItemGroupService stockItemGroupService;

    private StockItemGroupDTO generateStockItemGroupDTO(Boolean isActive) {
        return StockItemGroupDTO.builder()
                .name(RandomStringUtils.secure().nextAlphanumeric(33))
                .isActive(isActive)
                .build();
    }

    private List<StockItemGroup> generateGroups(int count, boolean isActive) {
        return Stream.generate(() -> generateStockItemGroup(isActive))
                .limit(count)
                .toList();
    }

    @Nested
    @DisplayName("save(stockItemGroupDTO)")
    class SaveTests {
        private static Stream<StockItemGroupDTO> invalidGroupData() {
            return Stream.of(StockItemGroupDTO.builder()
                            .build(),
                    StockItemGroupDTO.builder()
                            .name("")
                            .build(),
                    StockItemGroupDTO.builder()
                            .name("   ")
                            .build(),
                    null);
        }

        @Test
        @DisplayName("save_success_createsNewGroup")
        void save_success_createsNewGroup() {
            stockItemGroupRepository.deleteAll();

            StockItemGroupDTO stockItemGroupDTO = generateStockItemGroupDTO(true);

            StockItemGroup stockItemGroup = stockItemGroupService.save(stockItemGroupDTO);

            assertThat(stockItemGroup.getId())
                    .isNotNull();
            assertThat(stockItemGroup.getCode())
                    .isNotNull();
            assertThat(stockItemGroup.getName())
                    .isEqualTo(stockItemGroupDTO.getName());
            assertThat(stockItemGroup.getIsActive())
                    .isTrue();

            assertThat(stockItemGroupRepository.count())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("save_success_setsIsActiveFalseWhenDtoIsActiveNull")
        void save_success_setsIsActiveFalseWhenDtoIsActiveNull() {
            stockItemGroupRepository.deleteAll();

            StockItemGroupDTO stockItemGroupDTO = generateStockItemGroupDTO(null);

            StockItemGroup stockItemGroup = stockItemGroupService.save(stockItemGroupDTO);

            assertThat(stockItemGroup.getIsActive())
                    .isFalse();
        }

        @Test
        @DisplayName("save_success_setsIsActiveFalseWhenDtoIsActiveFalse")
        void save_success_setsIsActiveFalseWhenDtoIsActiveFalse() {
            stockItemGroupRepository.deleteAll();

            StockItemGroupDTO stockItemGroupDTO = generateStockItemGroupDTO(false);

            StockItemGroup stockItemGroup = stockItemGroupService.save(stockItemGroupDTO);

            assertThat(stockItemGroup.getIsActive())
                    .isFalse();
        }

        @Test
        @DisplayName("save_fail_whenNameAlreadyExists_throwsValidationException")
        void save_fail_whenNameAlreadyExists_throwsValidationException() {
            stockItemGroupRepository.deleteAll();

            String name = RandomStringUtils.secure().nextAlphanumeric(30);

            stockItemGroupRepository.save(StockItemGroup.builder()
                    .code(CodeGenerator.StockCodeGenerator.generate())
                    .name(name)
                    .isActive(true)
                    .build());

            StockItemGroupDTO stockItemGroupDTO = StockItemGroupDTO.builder()
                    .name(name)
                    .isActive(true)
                    .build();

            assertThatThrownBy(() -> stockItemGroupService.save(stockItemGroupDTO))
                    .isInstanceOf(ValidationException.class);

            assertThat(stockItemGroupRepository.count())
                    .isEqualTo(1);
        }

        @ParameterizedTest(name = "save_fail_whenInvalidObject'")
        @MethodSource("invalidGroupData")
        @DisplayName("save_fail_whenInvalidObject")
        void save_fail_whenInvalidObject(StockItemGroupDTO stockItemGroupDTO) {
            assertThatThrownBy(() -> stockItemGroupService.save(stockItemGroupDTO))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("findBy(code, isActive, pageSize, page)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_withCodeFilter_returnsMatchingGroupsByCodeLikeIgnoreCase")
        void findBy_success_withCodeFilter_returnsMatchingGroupsByCodeLikeIgnoreCase() {
            stockItemGroupRepository.deleteAll();

            StockItemGroup stockItemGroup = generateStockItemGroup(true);

            stockItemGroupRepository.save(StockItemGroup.builder()
                    .code(GENERATOR.nextAlphanumeric(30))
                    .name(RandomStringUtils.secure().nextAlphanumeric(10))
                    .isActive(true)
                    .build());

            List<StockItemGroup> result = stockItemGroupService.findBy(stockItemGroup.getCode(),
                    null,
                    10,
                    1);

            assertThat(result)
                    .extracting(StockItemGroup::getCode)
                    .contains(stockItemGroup.getCode());
        }

        @Test
        @DisplayName("findBy_success_withIsActiveTrueFilter_returnsOnlyActive")
        void findBy_success_withIsActiveTrueFilter_returnsOnlyActive() {
            stockItemGroupRepository.deleteAll();

            List<StockItemGroup> activeGroups = generateGroups(3, true);
            List<StockItemGroup> inactiveGroups = generateGroups(2, false);

            stockItemGroupRepository.saveAll(activeGroups);
            stockItemGroupRepository.saveAll(inactiveGroups);

            List<StockItemGroup> result = stockItemGroupService.findBy(null,
                    true,
                    10,
                    1);

            assertThat(result)
                    .isNotEmpty()
                    .allMatch(StockItemGroup::getIsActive);
        }

        @Test
        @DisplayName("findBy_success_withIsActiveFalseFilter_returnsOnlyInactive")
        void findBy_success_withIsActiveFalseFilter_returnsOnlyInactive() {
            stockItemGroupRepository.deleteAll();

            List<StockItemGroup> activeGroups = generateGroups(2,
                    true);
            List<StockItemGroup> inactiveGroups = generateGroups(3,
                    false);

            stockItemGroupRepository.saveAll(activeGroups);
            stockItemGroupRepository.saveAll(inactiveGroups);

            List<StockItemGroup> result = stockItemGroupService.findBy(null,
                    false,
                    10,
                    1);

            assertThat(result)
                    .isNotEmpty()
                    .allMatch(group -> !group.getIsActive());
        }

        @ParameterizedTest(name = "findBy_withNullOrBlankCode_returnsPage: code=\"{0}\"")
        @NullSource
        @ValueSource(strings = {"", " ", "\t"})
        @DisplayName("findBy_withNullOrBlankCode_returnsPage")
        void findBy_withNullOrBlankCode_returnsPage(String code) {
            stockItemGroupRepository.deleteAll();

            stockItemGroupRepository.saveAll(generateGroups(15, true));

            List<StockItemGroup> result = stockItemGroupService.findBy(code,
                    null,
                    10,
                    1);

            assertThat(result)
                    .isNotEmpty()
                    .hasSizeLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("findBy_success_paginatesResults")
        void findBy_success_paginatesResults() {
            stockItemGroupRepository.deleteAll();

            stockItemGroupRepository.saveAll(generateGroups(25, true));

            List<StockItemGroup> firstPage =
                    stockItemGroupService.findBy(null,
                            null,
                            10,
                            1);
            List<StockItemGroup> secondPage =
                    stockItemGroupService.findBy(null,
                            null,
                            10,
                            2);

            assertThat(firstPage)
                    .hasSizeLessThanOrEqualTo(10);
            assertThat(secondPage)
                    .hasSizeLessThanOrEqualTo(10);

            assertThat(firstPage)
                    .extracting(StockItemGroup::getId)
                    .doesNotContainAnyElementsOf(secondPage.stream()
                            .map(StockItemGroup::getId)
                            .toList());
        }

        @ParameterizedTest(name = "findBy_fail_whenPageSizeInvalid: pageSize={0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("findBy_fail_whenPageSizeInvalid")
        void findBy_fail_whenPageSizeInvalid(int invalidPageSize) {
            assertThatThrownBy(() -> stockItemGroupService.findBy(null,
                    null,
                    invalidPageSize,
                    1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "findBy_fail_whenPageInvalid: page={0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("findBy_fail_whenPageInvalid")
        void findBy_fail_whenPageInvalid(int invalidPage) {
            assertThatThrownBy(() -> stockItemGroupService.findBy(null,
                    null,
                    10,
                    invalidPage))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("update(groupId, groupName, isActive)")
    class UpdateTests {
        @Test
        @DisplayName("update_success_updatesNameAndIsActive")
        void update_success_updatesNameAndIsActive() {
            stockItemGroupRepository.deleteAll();

            StockItemGroup stockItemGroup = stockItemGroupRepository.save(generateStockItemGroup(true));

            String newName = RandomStringUtils.secure().nextAlphanumeric(33);

            StockItemGroup updated = stockItemGroupService.update(stockItemGroup.getId(),
                    newName,
                    false);

            assertThat(updated.getName())
                    .isEqualTo(newName);
            assertThat(updated.getIsActive())
                    .isFalse();

            StockItemGroup reloaded = stockItemGroupRepository.findById(stockItemGroup.getId())
                    .orElseThrow();

            assertThat(reloaded.getName())
                    .isEqualTo(newName);
            assertThat(reloaded.getIsActive())
                    .isFalse();
        }

        @Test
        @DisplayName("update_success_updatesOnlyNameWhenIsActiveNull")
        void update_success_updatesOnlyNameWhenIsActiveNull() {
            stockItemGroupRepository.deleteAll();

            StockItemGroup stockItemGroup = stockItemGroupRepository.save(generateStockItemGroup(true));

            String newName = RandomStringUtils.secure().nextAlphanumeric(33);

            StockItemGroup updated = stockItemGroupService.update(stockItemGroup.getId(),
                    newName,
                    null);

            assertThat(updated.getName())
                    .isEqualTo(newName);
            assertThat(updated.getIsActive())
                    .isTrue();
        }

        @Test
        @DisplayName("update_success_updatesOnlyIsActiveWhenNameBlank")
        void update_success_updatesOnlyIsActiveWhenNameBlank() {
            stockItemGroupRepository.deleteAll();

            StockItemGroup stockItemGroup = stockItemGroupRepository.save(generateStockItemGroup(true));

            StockItemGroup updated = stockItemGroupService.update(stockItemGroup.getId(),
                    "   ",
                    false);

            assertThat(updated.getName())
                    .isEqualTo(stockItemGroup.getName());
            assertThat(updated.getIsActive())
                    .isFalse();
        }

        @Test
        @DisplayName("update_fail_whenNotFound_throwsNotFoundException")
        void update_fail_whenNotFound_throwsNotFoundException() {
            stockItemGroupRepository.deleteAll();

            assertThatThrownBy(() -> stockItemGroupService.update(Long.MAX_VALUE,
                    null,
                    null))
                    .isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest(name = "update_fail_whenIdInvalid: id={0}")
        @NullSource
        @ValueSource(longs = {0L, -1L})
        @DisplayName("update_fail_whenIdInvalid")
        void update_fail_whenIdInvalid(Long id) {
            assertThatThrownBy(() -> stockItemGroupService.update(id,
                    null,
                    null))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
