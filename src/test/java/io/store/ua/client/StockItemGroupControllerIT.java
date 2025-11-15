package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.models.dto.StockItemGroupDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemGroupControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @Nested
    @DisplayName("GET /api/v1/stockItemGroups/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_returnsGroupByCodeFilter")
        void findBy_success_returnsGroupByCodeFilter() {
            StockItemGroup stockItemGroup = generateStockItemGroup(true);
            stockItemGroupRepository.save(stockItemGroup);

            String url = UriComponentsBuilder
                    .fromPath("/api/v1/stockItemGroups/findBy")
                    .queryParam("code", stockItemGroup.getCode())
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

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            StockItemGroup[] body = response.getBody();
            assertThat(body).isNotEmpty();

            StockItemGroup persisted = stockItemGroupRepository
                    .findByCode(stockItemGroup.getCode())
                    .orElseThrow();

            assertThat(body)
                    .anySatisfy(group -> {
                        assertThat(group.getId()).isEqualTo(persisted.getId());
                        assertThat(group.getCode()).isEqualTo(persisted.getCode());
                        assertThat(group.getName()).isEqualTo(persisted.getName());
                        assertThat(group.getIsActive()).isEqualTo(persisted.getIsActive());
                    });
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stockItemGroups")
    class SaveGroupTests {
        @Test
        @DisplayName("save_success_createsGroup_whenCodeMissing_usesName_andIsActiveFlag")
        void save_success_createsGroup_whenCodeMissing_usesName_andIsActiveFlag() {
            String groupName = GENERATOR.nextAlphabetic(30);

            StockItemGroupDTO stockItemGroupDTO = StockItemGroupDTO.builder()
                    .name(groupName)
                    .isActive(true)
                    .build();

            ResponseEntity<StockItemGroup> response = restClient.exchange("/api/v1/stockItemGroups",
                    HttpMethod.POST,
                    new HttpEntity<>(stockItemGroupDTO, authenticationHeaders),
                    StockItemGroup.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            StockItemGroup stockItemGroup = stockItemGroupRepository.findByName(groupName)
                    .orElseThrow();

            assertThat(response.getBody().getId())
                    .isEqualTo(stockItemGroup.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(stockItemGroup.getCode());
            assertThat(response.getBody().getName())
                    .isEqualTo(stockItemGroup.getName());
            assertThat(response.getBody().getIsActive())
                    .isEqualTo(stockItemGroup.getIsActive());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/stockItemGroups")
    class UpdateGroupTests {
        @Test
        @DisplayName("update_success_updatesFields")
        void update_success_updatesFields() {
            StockItemGroup stockItemGroup = generateStockItemGroup(true);
            String newName = GENERATOR.nextAlphabetic(330);

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemGroups")
                    .queryParam("groupId", stockItemGroup.getId())
                    .queryParam("name", newName)
                    .queryParam("isActive", false)
                    .build(true)
                    .toUriString();

            ResponseEntity<StockItemGroup> response = restClient.exchange(url,
                    HttpMethod.PUT,
                    new HttpEntity<>(authenticationHeaders),
                    StockItemGroup.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            stockItemGroup = stockItemGroupRepository.findById(stockItemGroup.getId())
                    .orElseThrow();

            assertThat(response.getBody().getId())
                    .isEqualTo(stockItemGroup.getId());
            assertThat(response.getBody().getCode())
                    .isEqualTo(stockItemGroup.getCode());
            assertThat(response.getBody().getName())
                    .isEqualTo(stockItemGroup.getName());
            assertThat(response.getBody().getIsActive())
                    .isEqualTo(stockItemGroup.getIsActive());
            assertThat(stockItemGroup.getName())
                    .isEqualTo(newName);
            assertThat(stockItemGroup.getIsActive())
                    .isFalse();
        }

        @Test
        @DisplayName("update_fails_missingId_returns4xx")
        void update_fails_missingId_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemGroups")
                    .queryParam("name", GENERATOR.nextAlphabetic(24))
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.PUT,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
