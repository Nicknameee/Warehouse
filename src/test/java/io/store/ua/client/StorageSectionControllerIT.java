package io.store.ua.client;

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

    private String buildFindByURL(Long warehouseId, int pageSize, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/api/v1/storage/sections/findBy")
                .queryParam("pageSize", pageSize)
                .queryParam("page", page);

        if (warehouseId != null) {
            builder.queryParam("warehouse_id", warehouseId);
        }

        return builder
                .build(true)
                .toUriString();
    }

    private String buildSaveUrl(Long warehouseId, String code) {
        return UriComponentsBuilder
                .fromPath("/api/v1/storage/sections")
                .queryParam("warehouse_id", warehouseId)
                .queryParam("code", code)
                .build(true)
                .toUriString();
    }

    private String buildUpdateUrl(Long sectionId, String newCode) {
        return UriComponentsBuilder
                .fromPath("/api/v1/storage/sections")
                .queryParam("id", sectionId)
                .queryParam("code", newCode)
                .build(true)
                .toUriString();
    }

    @Nested
    @DisplayName("GET /api/v1/storage/sections/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_filtersByWarehouse: filters by warehouse & pagination")
        void findBy_success_filtersByWarehouse() {
            Warehouse warehouse = generateWarehouse();
            Warehouse otherWarehouse = generateWarehouse();

            StorageSection sectionA = generateStorageSection(warehouse.getId());
            StorageSection sectionB = generateStorageSection(warehouse.getId());
            generateStorageSection(otherWarehouse.getId());

            String url = buildFindByURL(warehouse.getId(), 5, 1);

            ResponseEntity<List<StorageSection>> response = restClient.exchange(url,
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
                    .allSatisfy(section -> assertThat(section.getWarehouseId())
                            .isEqualTo(warehouse.getId()));
            assertThat(response.getBody()
                    .stream()
                    .map(StorageSection::getId))
                    .contains(sectionA.getId(), sectionB.getId());
        }

        @Test
        @DisplayName("findBy_success_returnsAllWhenNoWarehouse: without warehouseId → returns all paged")
        void findBy_success_returnsAllWhenNoWarehouse() {
            Warehouse warehouseA = generateWarehouse();
            Warehouse warehouseB = generateWarehouse();

            StorageSection section = generateStorageSection(warehouseA.getId());
            StorageSection otherSection = generateStorageSection(warehouseA.getId());
            StorageSection anotherSection = generateStorageSection(warehouseB.getId());

            ResponseEntity<List<StorageSection>> response = restClient.exchange(buildFindByURL(null, 10, 1),
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .isNotEmpty();
            assertThat(response.getBody()
                    .stream()
                    .map(StorageSection::getId))
                    .contains(section.getId(), otherSection.getId(), anotherSection.getId());
        }

        @Test
        @DisplayName("findBy_fails_invalidPagination: invalid pagination → 4xx")
        void findBy_fails_invalidPagination() {
            ResponseEntity<String> response = restClient.exchange(buildFindByURL(null, 0, 0),
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/storage/sections")
    class SaveTests {
        @Test
        @DisplayName("save_success_createsSection: creates section")
        void save_success_createsSection() {
            Warehouse warehouse = generateWarehouse();
            String code = RandomStringUtils.secure().nextAlphanumeric(8);

            ResponseEntity<StorageSection> response = restClient.exchange(buildSaveUrl(warehouse.getId(), code),
                    HttpMethod.POST,
                    new HttpEntity<>(authenticationHeaders),
                    StorageSection.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getWarehouseId())
                    .isEqualTo(warehouse.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(code);

            StorageSection storageSection = storageSectionRepository.findById(response.getBody().getId())
                    .orElseThrow();

            assertThat(storageSection.getWarehouseId())
                    .isEqualTo(warehouse.getId());
            assertThat(storageSection.getCode())
                    .isEqualTo(code);
        }

        @Test
        @DisplayName("duplicate in same warehouse → 4xx")
        void save_fails_duplicateSameWarehouse() {
            Warehouse warehouse = generateWarehouse();
            String duplicateCode = RandomStringUtils.secure().nextAlphanumeric(8);

            storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code(duplicateCode)
                    .isActive(true)
                    .build());

            String url = buildSaveUrl(warehouse.getId(), duplicateCode);

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("save_fails_sameCodeDifferentWarehouse: same code in different warehouses → allowed")
        void save_fails_sameCodeDifferentWarehouse() {
            Warehouse firstWarehouse = generateWarehouse();
            Warehouse otherWarehouse = generateWarehouse();
            String code = RandomStringUtils.secure().nextAlphanumeric(8);

            storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(firstWarehouse.getId())
                    .code(code)
                    .isActive(true)
                    .build());

            ResponseEntity<StorageSection> response = restClient.exchange(buildSaveUrl(otherWarehouse.getId(), code),
                    HttpMethod.POST,
                    new HttpEntity<>(authenticationHeaders),
                    StorageSection.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getWarehouseId())
                    .isEqualTo(otherWarehouse.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(code);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/storage/sections")
    class UpdateTests {
        @Test
        @DisplayName("update_success_updatesCode: updates code")
        void update_success_updatesCode() {
            Warehouse warehouse = generateWarehouse();

            StorageSection section = storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code(RandomStringUtils.secure().nextAlphanumeric(8))
                    .isActive(true)
                    .build());

            String newCode = RandomStringUtils.secure().nextAlphanumeric(8);

            ResponseEntity<StorageSection> response = restClient.exchange(buildUpdateUrl(section.getId(), newCode),
                    HttpMethod.PUT,
                    new HttpEntity<>(authenticationHeaders),
                    StorageSection.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getId())
                    .isEqualTo(section.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(newCode);

            StorageSection storageSection = storageSectionRepository.findById(section.getId())
                    .orElseThrow();

            assertThat(storageSection.getCode())
                    .isEqualTo(newCode);
        }

        @Test
        @DisplayName("update_fails_notFound: not found → 4xx")
        void update_fails_notFound() {
            ResponseEntity<String> response = restClient.exchange(buildUpdateUrl(Long.MAX_VALUE, RandomStringUtils.secure().nextAlphanumeric(8)),
                    HttpMethod.PUT,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("update_fails_notFound_fails_duplicateInWarehouse: duplicate in warehouse → 4xx")
        void update_fails_notFound_fails_duplicateInWarehouse() {
            Warehouse warehouse = generateWarehouse();

            StorageSection original = storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code("A")
                    .isActive(true)
                    .build());

            StorageSection conflict = storageSectionRepository.save(StorageSection.builder()
                    .warehouseId(warehouse.getId())
                    .code("B")
                    .isActive(true)
                    .build());

            ResponseEntity<String> response = restClient.exchange(buildUpdateUrl(original.getId(), conflict.getCode()),
                    HttpMethod.PUT,
                    new HttpEntity<>(authenticationHeaders),
                    String.class
            );

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
