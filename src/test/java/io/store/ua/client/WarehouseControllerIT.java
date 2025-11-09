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
import org.springframework.web.util.UriComponentsBuilder;

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
        @DisplayName("returns paginated list for OWNER/MANAGER")
        void findAll_success_returnsPaginatedList() {
            Warehouse first = generateWarehouse();
            generateWarehouse();
            Warehouse third = generateWarehouse();

            String firstPageUrl = UriComponentsBuilder.fromPath("/api/v1/warehouses")
                    .queryParam("pageSize", 1)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            String otherPageUrl = UriComponentsBuilder.fromPath("/api/v1/warehouses")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Warehouse>> firstPage = restClient.exchange(
                    firstPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );
            ResponseEntity<List<Warehouse>> otherPage = restClient.exchange(
                    otherPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(firstPage.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(otherPage.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody()).isNotNull().hasSize(1);
            assertThat(otherPage.getBody()).isNotNull().hasSize(1);

            Set<String> codes = firstPage.getBody().stream()
                    .map(Warehouse::getCode)
                    .collect(Collectors.toSet());
            codes.addAll(otherPage.getBody().stream().map(Warehouse::getCode).toList());

            assertThat(codes).contains(first.getCode(), third.getCode());

            String managerPageUrl = UriComponentsBuilder.fromPath("/api/v1/warehouses")
                    .queryParam("pageSize", 5)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Warehouse>> pageManager = restClient.exchange(
                    managerPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(managerHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(pageManager.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(pageManager.getBody()).isNotNull().isNotEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/warehouses/findBy/code")
    class FindByCodeTests {
        @Test
        @DisplayName("returns warehouse by code")
        void findByCode_success_returnsWarehouseByCode() {
            Warehouse created = generateWarehouse();

            String url = UriComponentsBuilder.fromPath("/api/v1/warehouses/findBy/code")
                    .queryParam("code", created.getCode())
                    .build(true)
                    .toUriString();

            ResponseEntity<Warehouse> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    Warehouse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(created.getCode());
            assertThat(response.getBody().getName()).isEqualTo(created.getName());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/warehouses")
    class SaveWarehouseTests {
        @Test
        @DisplayName("creates warehouse for OWNER only")
        void save_success_createsWarehouseForOwnerOnly() {
            WarehouseDTO warehouseDTO = buildWarehouseDTO();

            ResponseEntity<Warehouse> createResponse = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.POST,
                    new HttpEntity<>(warehouseDTO, ownerHeaders),
                    Warehouse.class
            );

            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(createResponse.getBody()).isNotNull();
            assertThat(createResponse.getBody().getCode()).isNotBlank();
            assertThat(createResponse.getBody().getName()).isEqualTo(warehouseDTO.getName());

            ResponseEntity<String> forbiddenResponse = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.POST,
                    new HttpEntity<>(buildWarehouseDTO(), managerHeaders),
                    String.class
            );

            assertThat(forbiddenResponse.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/warehouses")
    class UpdateWarehouseTests {
        @Test
        @DisplayName("updates mutable fields for OWNER/MANAGER; managerId only by OWNER")
        void update_success_updatesFieldsRespectsRoleRules() {
            Warehouse existing = generateWarehouse();

            WarehouseDTO ownerUpdate = WarehouseDTO.builder()
                    .code(existing.getCode())
                    .name("%s%s".formatted(existing.getName(), GENERATOR.nextAlphabetic(3)))
                    .isActive(Boolean.FALSE.equals(existing.getIsActive()) ? Boolean.TRUE : Boolean.FALSE)
                    .managerId(owner.getId())
                    .workingHours(existing.getWorkingHours())
                    .address(existing.getAddress())
                    .phones(existing.getPhones())
                    .build();

            ResponseEntity<Warehouse> ownerResponse = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerUpdate, ownerHeaders),
                    Warehouse.class
            );

            assertThat(ownerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            Warehouse afterOwnerUpdate = warehouseRepository.findByCode(existing.getCode()).orElseThrow();
            assertThat(afterOwnerUpdate).isNotNull();
            assertThat(afterOwnerUpdate.getName()).isEqualTo(ownerUpdate.getName());
            assertThat(afterOwnerUpdate.getIsActive()).isEqualTo(ownerUpdate.getIsActive());
            assertThat(afterOwnerUpdate.getManagerId()).isEqualTo(ownerUpdate.getManagerId());

            WarehouseDTO managerUpdate = WarehouseDTO.builder()
                    .code(existing.getCode())
                    .managerId(manager.getId())
                    .build();

            ResponseEntity<String> managerAttempt = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.PUT,
                    new HttpEntity<>(managerUpdate, managerHeaders),
                    String.class
            );

            assertThat(managerAttempt.getStatusCode().is4xxClientError()).isTrue();
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

            String toggleUrlOwner = UriComponentsBuilder.fromPath("/api/v1/warehouses/toggle")
                    .queryParam("code", warehouse.getCode())
                    .build(true)
                    .toUriString();

            ResponseEntity<Warehouse> toggleResponse = restClient.exchange(
                    toggleUrlOwner,
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerHeaders),
                    Warehouse.class
            );

            assertThat(toggleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(toggleResponse.getBody()).isNotNull();
            assertThat(toggleResponse.getBody().getIsActive()).isEqualTo(!initial);

            String toggleUrlManager = UriComponentsBuilder.fromPath("/api/v1/warehouses/toggle")
                    .queryParam("code", warehouse.getCode())
                    .build(true)
                    .toUriString();

            ResponseEntity<Warehouse> toggleBack = restClient.exchange(
                    toggleUrlManager,
                    HttpMethod.PUT,
                    new HttpEntity<>(managerHeaders),
                    Warehouse.class
            );

            assertThat(toggleBack.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(toggleBack.getBody()).isNotNull();
            assertThat(toggleBack.getBody().getIsActive()).isEqualTo(initial);
        }
    }
}