package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.models.dto.StockItemGroupDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemGroupControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @Nested
    @DisplayName("GET /api/v1/stockItemGroups/findAll")
    class FindAllGroupsTests {
        @Test
        @DisplayName("findAll_success_returnsPaginatedList")
        void findAll_success_returnsPaginatedList() {
            StockItemGroup firstGroup = generateStockItemGroup(true);
            StockItemGroup otherGroup = generateStockItemGroup(true);
            StockItemGroup anotherGroup = generateStockItemGroup(false);

            String firstPageUrl = UriComponentsBuilder.fromPath("/api/v1/stockItemGroups/findAll")
                    .queryParam("pageSize", 1)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            String tailPageUrl = UriComponentsBuilder.fromPath("/api/v1/stockItemGroups/findAll")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItemGroup>> firstPage = restClient.exchange(
                    firstPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });
            ResponseEntity<List<StockItemGroup>> tailPage = restClient.exchange(
                    tailPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(firstPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(tailPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(tailPage.getBody())
                    .isNotNull()
                    .hasSize(1);

            Set<Long> returnedIds = firstPage.getBody()
                    .stream()
                    .map(StockItemGroup::getId)
                    .collect(Collectors.toSet());
            returnedIds.addAll(tailPage.getBody()
                    .stream()
                    .map(StockItemGroup::getId)
                    .toList());

            assertThat(returnedIds)
                    .containsAnyOf(firstGroup.getId(), otherGroup.getId(), anotherGroup.getId());
        }

        @Test
        @DisplayName("findAll_fails_invalidPagination_returns4xx")
        void findAll_fails_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemGroups/findAll")
                    .queryParam("pageSize", 0)
                    .queryParam("page", -1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stockItemGroups/findBy/code")
    class FindByCodeTests {
        @Test
        @DisplayName("findByCode_success_returnsGroupByCode")
        void findByCode_success_returnsGroupByCode() {
            StockItemGroup stockItemGroup = generateStockItemGroup(true);

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemGroups/findBy/code")
                    .queryParam("code", stockItemGroup.getCode())
                    .build(true)
                    .toUriString();

            ResponseEntity<StockItemGroup> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    StockItemGroup.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            StockItemGroup persisted = stockItemGroupRepository.findByCode(stockItemGroup.getCode())
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
                    .queryParam("is_active", false)
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
