package io.store.ua.controllers;

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

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseControllerIT extends AbstractIT {
    private static final String MANAGER = "manager";
    private HttpHeaders ownerHeaders;
    private HttpHeaders managerHeaders;
    private User manager;

    @BeforeAll
    void setupAuthentication() {
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

    private HttpHeaders jsonOwnerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(ownerHeaders);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders jsonManagerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(managerHeaders);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Nested
    @DisplayName("GET /api/v1/warehouses/findBy")
    class FindByWarehousesTests {
        @Test
        @DisplayName("findBy_success_appliesAllFilters")
        void findBy_success_appliesAllFilters() {
            Warehouse target = generateWarehouse();
            target.setCode("ALPHA-" + target.getCode());
            target.setName("Alpha " + target.getName());
            target.setManagerId(manager.getId());
            target.setIsActive(true);
            warehouseRepository.save(target);

            Warehouse otherCode = generateWarehouse();
            otherCode.setCode("BETA-" + otherCode.getCode());
            otherCode.setName("Alpha " + otherCode.getName());
            otherCode.setManagerId(manager.getId());
            otherCode.setIsActive(true);
            warehouseRepository.save(otherCode);

            Warehouse otherName = generateWarehouse();
            otherName.setCode("ALPHA-" + otherName.getCode());
            otherName.setName("Gamma " + otherName.getName());
            otherName.setManagerId(manager.getId());
            otherName.setIsActive(true);
            warehouseRepository.save(otherName);

            Warehouse otherManager = generateWarehouse();
            otherManager.setCode("ALPHA-" + otherManager.getCode());
            otherManager.setName("Alpha " + otherManager.getName());
            otherManager.setIsActive(true);
            warehouseRepository.save(otherManager);

            Warehouse inactive = generateWarehouse();
            inactive.setCode("ALPHA-" + inactive.getCode());
            inactive.setName("Alpha " + inactive.getName());
            inactive.setManagerId(manager.getId());
            inactive.setIsActive(false);
            warehouseRepository.save(inactive);

            String url = UriComponentsBuilder.fromPath("/api/v1/warehouses/findBy")
                    .queryParam("codePrefix", "ALPHA-")
                    .queryParam("namePrefix", "Alpha")
                    .queryParam("managerId", manager.getId())
                    .queryParam("isActive", true)
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Warehouse>> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(Warehouse::getId)
                    .containsExactly(target.getId());
            assertThat(response.getBody())
                    .allSatisfy(wh -> {
                        assertThat(wh.getCode()).startsWith("ALPHA-");
                        assertThat(wh.getName()).startsWith("Alpha");
                        assertThat(wh.getManagerId()).isEqualTo(manager.getId());
                        assertThat(wh.getIsActive()).isTrue();
                    });
        }

        @Test
        @DisplayName("findBy_fail_invalidPagination_returns4xx")
        void findBy_fail_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/warehouses/findBy")
                    .queryParam("pageSize", 0)
                    .queryParam("page", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    String.class
            );

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/warehouses")
    class SaveWarehouseTests {
        @Test
        @DisplayName("save_success_createsWarehouseForOwnerOnly")
        void save_success_createsWarehouseForOwnerOnly() {
            WarehouseDTO warehouseDTO = buildWarehouseDTO();

            ResponseEntity<Warehouse> createResponse = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.POST,
                    new HttpEntity<>(warehouseDTO, jsonOwnerHeaders()),
                    Warehouse.class
            );

            assertThat(createResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(createResponse.getBody())
                    .isNotNull();
            assertThat(createResponse.getBody().getCode())
                    .isNotBlank();
            assertThat(createResponse.getBody().getName())
                    .isEqualTo(warehouseDTO.getName());

            ResponseEntity<String> forbiddenResponse = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.POST,
                    new HttpEntity<>(buildWarehouseDTO(), jsonManagerHeaders()),
                    String.class
            );

            assertThat(forbiddenResponse.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/warehouses")
    class UpdateWarehouseTests {
        @Test
        @DisplayName("update_success_updatesFieldsRespectsRoleRules")
        void update_success_updatesFieldsRespectsRoleRules() {
            Warehouse warehouse = generateWarehouse();

            WarehouseDTO warehouseDTO = WarehouseDTO.builder()
                    .code(warehouse.getCode())
                    .name("%s%s".formatted(warehouse.getName(), GENERATOR.nextAlphabetic(3)))
                    .isActive(Boolean.FALSE.equals(warehouse.getIsActive()) ? Boolean.TRUE : Boolean.FALSE)
                    .managerId(owner.getId())
                    .workingHours(warehouse.getWorkingHours())
                    .address(warehouse.getAddress())
                    .phones(warehouse.getPhones())
                    .build();

            ResponseEntity<Warehouse> response = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.PUT,
                    new HttpEntity<>(warehouseDTO, jsonOwnerHeaders()),
                    Warehouse.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            warehouse = warehouseRepository.findByCode(warehouse.getCode())
                    .orElseThrow();

            assertThat(warehouse)
                    .isNotNull();
            assertThat(warehouse.getName())
                    .isEqualTo(warehouseDTO.getName());
            assertThat(warehouse.getIsActive())
                    .isEqualTo(warehouseDTO.getIsActive());
            assertThat(warehouse.getManagerId())
                    .isEqualTo(warehouseDTO.getManagerId());

            warehouseDTO = WarehouseDTO.builder()
                    .code(warehouse.getCode())
                    .managerId(manager.getId())
                    .build();

            ResponseEntity<String> forbiddenResponse = restClient.exchange(
                    "/api/v1/warehouses",
                    HttpMethod.PUT,
                    new HttpEntity<>(warehouseDTO, jsonManagerHeaders()),
                    String.class
            );

            assertThat(forbiddenResponse.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/warehouses/toggle")
    class ToggleWarehouseStateTests {
        @Test
        @DisplayName("toggleState_success_togglesActiveStateForOwnerAndManager")
        void toggleState_success_togglesActiveStateForOwnerAndManager() {
            Warehouse warehouse = generateWarehouse();
            Boolean isActive = warehouse.getIsActive();

            String url = UriComponentsBuilder.fromPath("/api/v1/warehouses/toggle")
                    .queryParam("code", warehouse.getCode())
                    .build(true)
                    .toUriString();

            ResponseEntity<Warehouse> toggleResponse = restClient.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerHeaders),
                    Warehouse.class
            );

            assertThat(toggleResponse.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(toggleResponse.getBody())
                    .isNotNull();
            assertThat(toggleResponse.getBody().getIsActive())
                    .isEqualTo(!isActive);

            ResponseEntity<Warehouse> toggleBack = restClient.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(managerHeaders),
                    Warehouse.class
            );

            assertThat(toggleBack.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(toggleBack.getBody())
                    .isNotNull();
            assertThat(toggleBack.getBody().getIsActive())
                    .isEqualTo(isActive);
        }
    }
}
