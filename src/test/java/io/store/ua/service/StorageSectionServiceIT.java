package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.StorageSection;
import io.store.ua.entity.Warehouse;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.exceptions.UniqueCheckException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageSectionServiceIT extends AbstractIT {
    @Autowired
    private StorageSectionService storageSectionService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = generateWarehouse();
    }

    private List<StorageSection> generateSections(int count) {
        return generateSections(warehouse.getId(), count);
    }

    private List<StorageSection> generateSections(long warehouseId, int count) {
        return Stream.generate(() -> generateStorageSection(warehouseId))
                .limit(count)
                .toList();
    }

    @Nested
    @DisplayName("findBy(warehouseId: Long|null, pageSize: int, page: int)")
    class FindByTests {
        @ParameterizedTest(name = "findBy_success_specificWarehouse_paged: pageSize={0}, page={1}")
        @CsvSource({"1,1", "2,1", "2,2"})
        @Transactional
        void findBy_success_specificWarehouse_paged(int pageSize, int page) {
            storageSectionRepository.saveAll(generateSections(5));
            storageSectionRepository.saveAll(generateSections(generateWarehouse().getId(), 3));

            var result = storageSectionService.findBy(warehouse.getId(), true, pageSize, page);

            assertThat(result).hasSizeLessThanOrEqualTo(pageSize);
            assertThat(result).allMatch(storageSection -> storageSection.getWarehouseId().equals(warehouse.getId()));
        }

        @Test
        @DisplayName("findBy_success_nullWarehouse_returnsAllPaginated")
        @Transactional
        void findBy_success_nullWarehouse_returnsAllPaginated() {
            storageSectionRepository.saveAll(generateSections(3));
            storageSectionRepository.saveAll(generateSections(generateWarehouse().getId(), 3));

            var firstBatch = storageSectionService.findBy(null, null, 3, 1);
            var otherBatch = storageSectionService.findBy(null, null, 3, 2);

            assertThat(firstBatch).hasSize(3);
            assertThat(otherBatch).hasSize(3);
            assertThat(firstBatch).doesNotContainAnyElementsOf(otherBatch);
        }

        @ParameterizedTest(name = "findBy_fail_whenPageSizeInvalid: pageSize={0}")
        @ValueSource(ints = {0, -1, -5})
        void findBy_fail_whenPageSizeInvalid(int pageSize) {
            assertThatThrownBy(() -> storageSectionService.findBy(null, null, pageSize, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "findBy_fail_whenPageInvalid: page={0}")
        @ValueSource(ints = {0, -1, -10})
        void findBy_fail_whenPageInvalid(int page) {
            assertThatThrownBy(() -> storageSectionService.findBy(null, null, 10, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("save(warehouseId: long, code: string)")
    class SaveTests {
        @Test
        @DisplayName("save_success: creates a new StorageSection when (warehouseId, code) is unique")
        void save_success() {
            long initialCount = storageSectionRepository.count();
            String code = GENERATOR.nextAlphanumeric(10);

            StorageSection saved = storageSectionService.save(warehouse.getId(), code);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getWarehouseId()).isEqualTo(warehouse.getId());
            assertThat(saved.getCode()).isEqualTo(code);
            assertThat(storageSectionRepository.count()).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("save_fail_whenDuplicateInSameWarehouse: throws UniqueCheckException")
        void save_fail_duplicateSameWarehouse() {
            String code = GENERATOR.nextAlphanumeric(10);
            storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code(code)
                    .isActive(true)
                    .build());

            assertThatThrownBy(() -> storageSectionService.save(warehouse.getId(), code))
                    .isInstanceOf(UniqueCheckException.class);
        }

        @Test
        @DisplayName("save_success_sameCodeDifferentWarehouse: allowed")
        void save_success_sameCodeDifferentWarehouse() {
            String code = GENERATOR.nextAlphanumeric(10);
            storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(generateWarehouse().getId())
                    .code(code)
                    .isActive(true)
                    .build());

            StorageSection saved = storageSectionService.save(warehouse.getId(), code);

            assertThat(saved.getId())
                    .isNotNull();
            assertThat(saved.getWarehouseId())
                    .isEqualTo(warehouse.getId());
            assertThat(saved.getCode())
                    .isEqualTo(code);
        }

        @ParameterizedTest(name = "save_fail_whenWarehouseIdInvalid: warehouseId={0}")
        @NullSource
        @ValueSource(longs = {0, -1})
        void save_fail_whenWarehouseIdInvalid(Long warehouseId) {
            assertThatThrownBy(() -> storageSectionService.save(warehouseId, GENERATOR.nextAlphanumeric(333)))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "save_fail_whenCodeInvalid: code=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void save_fail_whenCodeInvalid(String code) {
            assertThatThrownBy(() -> storageSectionService.save(1L, code))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("update(sectionId: long, newCode: string)")
    class UpdateTests {
        @Test
        @DisplayName("update_success: changes code when new one is unique within the warehouse")
        @Transactional
        void update_success() {
            var existing = storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code(GENERATOR.nextAlphanumeric(8))
                    .isActive(true)
                    .build());

            String newCode = GENERATOR.nextAlphanumeric(9);

            StorageSection updated = storageSectionService.update(existing.getId(), false, newCode);

            assertThat(updated.getCode())
                    .isEqualTo(newCode);
            assertThat(updated.getIsActive())
                    .isFalse();
            assertThat(storageSectionRepository.findById(existing.getId()))
                    .isPresent().get().extracting(StorageSection::getCode).isEqualTo(newCode);
        }

        @Test
        @DisplayName("update_fail_whenNotFound: throws NotFoundException")
        void update_fail_whenNotFound() {
            assertThatThrownBy(() -> storageSectionService.update(Long.MAX_VALUE, null, "X1"))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("update_fail_whenDuplicateInSameWarehouse: throws UniqueCheckException")
        void update_fail_duplicateSameWarehouse() {
            var target = storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code("OLD_CODE")
                    .isActive(true)
                    .build());
            var conflict = storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code("USED")
                    .isActive(true)
                    .build());

            assertThat(conflict.getId()).isNotNull();

            assertThatThrownBy(() -> storageSectionService.update(target.getId(), null, "USED"))
                    .isInstanceOf(UniqueCheckException.class);
        }

        @ParameterizedTest(name = "update_fail_whenSectionIdInvalid: sectionId={0}")
        @NullSource
        @ValueSource(longs = {0, -1})
        void update_fail_whenSectionIdInvalid(Long sectionId) {
            assertThatThrownBy(() -> storageSectionService.update(sectionId, null, "C1"))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "update_fail_whenCodeInvalid: code=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void update_fail_whenCodeInvalid(String code) {
            assertThatThrownBy(() -> storageSectionService.update(1L, null, code))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
