package io.store.ua.controller;

import io.store.ua.AbstractIT;
import io.store.ua.entity.StorageSection;
import io.store.ua.entity.Warehouse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StorageSectionControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    @BeforeAll
    void setUp() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    private String buildFindByUrl(Long warehouseId, Boolean isActive, int pageSize, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/api/v1/storage/sections/findBy")
                .queryParam("pageSize", pageSize)
                .queryParam("page", page);

        if (warehouseId != null) {
            builder.queryParam("warehouseId", warehouseId);
        }
        if (isActive != null) {
            builder.queryParam("isActive", isActive);
        }

        return builder.build(true).toUriString();
    }

    private String buildSaveUrl(Long warehouseId, String code) {
        return UriComponentsBuilder
                .fromPath("/api/v1/storage/sections")
                .queryParam("warehouseId", warehouseId)
                .queryParam("code", code)
                .build(true)
                .toUriString();
    }

    private String buildUpdateUrl(Long sectionId, Boolean isActive, String newCode) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/api/v1/storage/sections")
                .queryParam("id", sectionId)
                .queryParam("code", newCode);

        if (isActive != null) {
            builder.queryParam("isActive", isActive);
        }

        return builder.build(true).toUriString();
    }

    @Nested
    @DisplayName("GET /api/v1/storage/sections/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_allFilters_applied")
        void findBy_success_allFilters_applied() {
            Warehouse warehouse = generateWarehouse();
            Warehouse otherWarehouse = generateWarehouse();

            StorageSection sectionA = generateStorageSection(warehouse.getId());
            sectionA.setIsActive(true);
            storageSectionRepository.save(sectionA);

            StorageSection sectionB = generateStorageSection(warehouse.getId());
            sectionB.setIsActive(true);
            storageSectionRepository.save(sectionB);

            StorageSection otherSection = generateStorageSection(otherWarehouse.getId());
            otherSection.setIsActive(false);
            storageSectionRepository.save(otherSection);

            String url = buildFindByUrl(warehouse.getId(), true, 10, 1);

            ResponseEntity<List<StorageSection>> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .isNotEmpty();
            assertThat(response.getBody())
                    .allSatisfy(section -> {
                        assertThat(section.getWarehouseId()).isEqualTo(warehouse.getId());
                        assertThat(section.getIsActive()).isTrue();
                    });
            assertThat(response.getBody()
                    .stream()
                    .map(StorageSection::getId))
                    .contains(sectionA.getId(), sectionB.getId());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/storage/sections")
    class SaveTests {
        @Test
        @DisplayName("save_success_createsSection_allParams")
        void save_success_createsSection_allParams() {
            Warehouse warehouse = generateWarehouse();
            String code = RandomStringUtils.secure().nextAlphanumeric(8);

            ResponseEntity<StorageSection> response = restClient.exchange(
                    buildSaveUrl(warehouse.getId(), code),
                    HttpMethod.POST,
                    new HttpEntity<>(authenticationHeaders),
                    StorageSection.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getWarehouseId())
                    .isEqualTo(warehouse.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(code);

            StorageSection persisted = storageSectionRepository.findById(response.getBody().getId())
                    .orElseThrow();

            assertThat(persisted.getWarehouseId())
                    .isEqualTo(warehouse.getId());
            assertThat(persisted.getCode())
                    .isEqualTo(code);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/storage/sections")
    class UpdateTests {
        @Test
        @DisplayName("update_success_updatesCodeAndIsActive_allParams")
        void update_success_updatesCodeAndIsActive_allParams() {
            Warehouse warehouse = generateWarehouse();
            StorageSection section = storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code(RandomStringUtils.secure().nextAlphanumeric(8))
                    .isActive(true)
                    .build());

            String newCode = RandomStringUtils.secure().nextAlphanumeric(8);

            ResponseEntity<StorageSection> response = restClient.exchange(
                    buildUpdateUrl(section.getId(), false, newCode),
                    HttpMethod.PUT,
                    new HttpEntity<>(authenticationHeaders),
                    StorageSection.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getId())
                    .isEqualTo(section.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(newCode);
            assertThat(response.getBody().getIsActive())
                    .isFalse();

            StorageSection reloaded = storageSectionRepository.findById(section.getId())
                    .orElseThrow();

            assertThat(reloaded.getCode())
                    .isEqualTo(newCode);
            assertThat(reloaded.getIsActive())
                    .isFalse();
        }
    }
}
