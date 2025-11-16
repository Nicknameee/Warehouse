package io.store.ua.controller;

import io.store.ua.AbstractIT;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.models.dto.StockItemGroupDTO;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemGroupControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @AfterEach
    void cleanUp() {
        stockItemGroupRepository.deleteAll();
    }

    private HttpHeaders generateHeaders(HttpHeaders original) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(original);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Nested
    @DisplayName("GET /api/v1/stockItemGroups/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_appliesCodePartAndIsActiveAndPagination")
        void findBy_success_appliesCodePartAndIsActiveAndPagination() {
            StockItemGroup activeGroup = stockItemGroupRepository.save(generateStockItemGroup(true));
            stockItemGroupRepository.save(generateStockItemGroup(false));

            String url = UriComponentsBuilder
                    .fromPath("/api/v1/stockItemGroups/findBy")
                    .queryParam("codePart", activeGroup.getCode().substring(0, 3))
                    .queryParam("isActive", true)
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<StockItemGroup[]> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    StockItemGroup[].class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .isNotEmpty()
                    .anySatisfy(group -> {
                        assertThat(group.getId()).isEqualTo(activeGroup.getId());
                        assertThat(group.getCode()).isEqualTo(activeGroup.getCode());
                        assertThat(group.getName()).isEqualTo(activeGroup.getName());
                        assertThat(group.getIsActive()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stockItemGroups")
    class SaveGroupTests {
        @Test
        @DisplayName("save_success_createsGroup")
        void save_success_createsGroup() {
            String groupName = GENERATOR.nextAlphabetic(30);

            StockItemGroupDTO stockItemGroupDTO = StockItemGroupDTO.builder()
                    .name(groupName)
                    .isActive(true)
                    .build();

            ResponseEntity<StockItemGroup> response = restClient.exchange(
                    "/api/v1/stockItemGroups",
                    HttpMethod.POST,
                    new HttpEntity<>(stockItemGroupDTO, generateHeaders(authenticationHeaders)),
                    StockItemGroup.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            StockItemGroup persisted = stockItemGroupRepository.findByName(groupName)
                    .orElseThrow();

            assertThat(response.getBody().getId())
                    .isEqualTo(persisted.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(persisted.getCode());
            assertThat(response.getBody().getName())
                    .isEqualTo(persisted.getName());
            assertThat(response.getBody().getIsActive())
                    .isEqualTo(persisted.getIsActive());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/stockItemGroups")
    class UpdateGroupTests {
        @Test
        @DisplayName("update_success_updatesFields")
        void update_success_updatesFields() {
            StockItemGroup stockItemGroup = stockItemGroupRepository.save(generateStockItemGroup(true));
            String newName = GENERATOR.nextAlphabetic(30);

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemGroups")
                    .queryParam("groupId", stockItemGroup.getId())
                    .queryParam("name", newName)
                    .queryParam("isActive", false)
                    .build(true)
                    .toUriString();

            ResponseEntity<StockItemGroup> response = restClient.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(authenticationHeaders),
                    StockItemGroup.class
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            StockItemGroup reloaded = stockItemGroupRepository.findById(stockItemGroup.getId())
                    .orElseThrow();

            assertThat(response.getBody().getId())
                    .isEqualTo(reloaded.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(reloaded.getCode());
            assertThat(response.getBody().getName())
                    .isEqualTo(reloaded.getName());
            assertThat(response.getBody().getIsActive())
                    .isEqualTo(reloaded.getIsActive());
            assertThat(reloaded.getName())
                    .isEqualTo(newName);
            assertThat(reloaded.getIsActive())
                    .isFalse();
        }
    }
}
