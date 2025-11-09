package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.User;
import io.store.ua.entity.Warehouse;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.models.dto.WarehouseDTO;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseControllerIT extends AbstractIT {
    private static final String MANAGER = "manager";
    private HttpHeaders ownerHeaders;
    private HttpHeaders managerHeaders;
    private User manager;

    @BeforeAll
    void setupAuth() {
        ownerHeaders = generateAuthenticationHeaders();
        managerHeaders = generateAuthenticationHeaders(MANAGER, MANAGER);
    }

    @BeforeEach
    void setUp() {
        manager = userRepository.save(User.builder()
                .username(MANAGER)
                .password(passwordEncoder.encode(MANAGER))
                .email("%s@example.com".formatted(MANAGER))
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .timezone("UTC")
                .build());
    }

    @Nested
    @DisplayName("GET /api/v1/warehouses")
    class FindAllWarehousesTests {
        @Test
        @DisplayName("findAll_success_returnsPaginatedList: returns paginated list for OWNER/MANAGER")
        void findAll_success_returnsPaginatedList() {
            Warehouse first = generateWarehouse();
            Warehouse second = generateWarehouse();
            Warehouse third = generateWarehouse();

            ResponseEntity<List<Warehouse>> firstPage = restClient.exchange("/api/v1/warehouses?pageSize=1&page=1",
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });
            ResponseEntity<List<Warehouse>> otherPage = restClient.exchange("/api/v1/warehouses?pageSize=2&page=2",
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(firstPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(otherPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(otherPage.getBody())
                    .isNotNull()
                    .hasSize(1);

            Set<String> codes = firstPage.getBody()
                    .stream()
                    .map(Warehouse::getCode)
                    .collect(Collectors.toSet());
            codes.addAll(otherPage.getBody()
                    .stream()
                    .map(Warehouse::getCode)
                    .toList());
            assertThat(codes)
                    .contains(first.getCode(), third.getCode());

            ResponseEntity<List<Warehouse>> pageManager = restClient.exchange("/api/v1/warehouses?pageSize=5&page=1",
                    HttpMethod.GET,
                    new HttpEntity<>(managerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(pageManager.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(pageManager.getBody())
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/warehouses/findBy/code")
    class FindByCodeTests {
        @Test
        @DisplayName("findByl_success_returnsWarehouseByCode: returns warehouse by code")
        void findByl_success_returnsWarehouseByCode() {
            Warehouse created = generateWarehouse();

            ResponseEntity<Warehouse> response = restClient.exchange("/api/v1/warehouses/findBy/code?code=%s".formatted(created.getCode()),
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    Warehouse.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody().getCode())
                    .isEqualTo(created.getCode());
            assertThat(response.getBody().getName())
                    .isEqualTo(created.getName());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/warehouses")
    class SaveWarehouseTests {
        @Test
        @DisplayName("creates warehouse for OWNER only")
        void save_success_createsWarehouseForOwnerOnly() {
            WarehouseDTO warehouseDTO = buildWarehouseDTO();

            ResponseEntity<Warehouse> createResponse = restClient.exchange("/api/v1/warehouses",
                    HttpMethod.POST,
                    new HttpEntity<>(warehouseDTO, ownerHeaders),
                    Warehouse.class);

            assertThat(createResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(createResponse.getBody())
                    .isNotNull();
            assertThat(createResponse.getBody().getCode())
                    .isNotBlank();
            assertThat(createResponse.getBody().getName())
                    .isEqualTo(warehouseDTO.getName());

            ResponseEntity<String> forbiddenResponse = restClient.exchange("/api/v1/warehouses",
                    HttpMethod.POST,
                    new HttpEntity<>(buildWarehouseDTO(), managerHeaders),
                    String.class);

            assertThat(forbiddenResponse.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/warehouses")
    class UpdateWarehouseTests {
        @Test
        @DisplayName("update_success_updatesFieldsRespectsRoleRules: updates mutable fields for OWNER/MANAGER; managerId only by OWNER")
        void update_success_updatesFieldsRespectsRoleRules() {
            Warehouse existing = generateWarehouse();

            WarehouseDTO ownerUpdate = WarehouseDTO.builder()
                    .code(existing.getCode())
                    .name("%s%s".formatted(existing.getName(), GENERATOR.nextAlphabetic(3)))
                    .isActive(Boolean.FALSE.equals(existing.getIsActive())
                            ? Boolean.TRUE : Boolean.FALSE)
                    .managerId(owner.getId())
                    .workingHours(existing.getWorkingHours())
                    .address(existing.getAddress())
                    .phones(existing.getPhones())
                    .build();

            ResponseEntity<Warehouse> ownerResponse = restClient.exchange("/api/v1/warehouses",
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerUpdate, ownerHeaders),
                    Warehouse.class
            );

            assertThat(ownerResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            Warehouse afterOwnerUpdate = warehouseRepository.findByCode(existing.getCode())
                    .orElseThrow();
            assertThat(afterOwnerUpdate)
                    .isNotNull();
            assertThat(afterOwnerUpdate.getName())
                    .isEqualTo(ownerUpdate.getName());
            assertThat(afterOwnerUpdate.getIsActive())
                    .isEqualTo(ownerUpdate.getIsActive());
            assertThat(afterOwnerUpdate.getManagerId())
                    .isEqualTo(ownerUpdate.getManagerId());

            WarehouseDTO managerUpdate = WarehouseDTO.builder()
                    .code(existing.getCode())
                    .managerId(manager.getId())
                    .build();

            ResponseEntity<String> managerAttempt = restClient.exchange("/api/v1/warehouses",
                    HttpMethod.PUT,
                    new HttpEntity<>(managerUpdate, managerHeaders),
                    String.class
            );

            assertThat(managerAttempt.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/warehouses/toggle")
    class ToggleWarehouseStateTests {
        @Test
        @DisplayName("toggles active state for OWNER/MANAGER")
        void toggleState_success_togglesActiveState() {
            Warehouse warehouse = generateWarehouse();
            Boolean initial = warehouse.getIsActive();

            ResponseEntity<Warehouse> toggleResponse = restClient.exchange("/api/v1/warehouses/toggle?code=%s"
                            .formatted(warehouse.getCode()),
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerHeaders),
                    Warehouse.class);

            assertThat(toggleResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(toggleResponse.getBody())
                    .isNotNull();
            assertThat(toggleResponse.getBody().getIsActive()).
                    isEqualTo(!initial);

            ResponseEntity<Warehouse> toggleBack = restClient.exchange("/api/v1/warehouses/toggle?code=%s"
                            .formatted(warehouse.getCode()),
                    HttpMethod.PUT,
                    new HttpEntity<>(managerHeaders),
                    Warehouse.class);

            assertThat(toggleBack.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(toggleBack.getBody())
                    .isNotNull();
            assertThat(toggleBack.getBody().getIsActive())
                    .isEqualTo(initial);
        }
    }
}
