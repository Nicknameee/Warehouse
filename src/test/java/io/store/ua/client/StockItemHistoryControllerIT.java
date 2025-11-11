package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Product;
import io.store.ua.entity.StockItem;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.entity.Warehouse;
import io.store.ua.entity.immutable.StockItemHistory;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemHistoryControllerIT extends AbstractIT {
    private static final DateTimeFormatter DMY_HMS = DateTimeFormatter.ofPattern("dd-MM-yyyy'At'HH:mm:ss");

    private HttpHeaders authHeaders;

    private Warehouse warehouseB;
    private Product product;
    private StockItemGroup groupA;
    private StockItem stockItem;

    @BeforeAll
    void setupAuthentication() {
        authHeaders = generateAuthenticationHeaders();
    }

    @BeforeEach
    void setUp() {
        warehouseB = generateWarehouse();
        product = generateProduct();
        groupA = generateStockItemGroup(true);
        stockItem = generateStockItem(product.getId(), groupA.getId(), generateWarehouse().getId());
    }

    @Nested
    @DisplayName("GET /api/v1/stockItemsHistory/findBy")
    class FindByTests {

        @Test
        @DisplayName("findBy_success_filtersByStockItemIdAndDateRange_pagination")
        void findBy_success_filtersByStockItemIdAndDateRange_pagination() {
            LocalDate d5 = LocalDate.now().plusDays(5);
            LocalDate d10 = LocalDate.now().plusDays(10);
            LocalDate d30 = LocalDate.now().plusDays(30);

            stockItemHistoryRepository.saveAll(List.of(
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .oldExpiration(d5)
                            .newExpiration(d10)
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .oldExpiration(d10)
                            .newExpiration(d30)
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build(),
                    StockItemHistory.builder()
                            .stockItemId(generateStockItem(product.getId(), groupA.getId(), warehouseB.getId()).getId())
                            .currentProductPrice(product.getPrice())
                            .oldExpiration(d10)
                            .newExpiration(d30)
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build()
            ));

            String fromStr = DMY_HMS.format(d10.atStartOfDay());
            String toStr = DMY_HMS.format(d30.atTime(23, 59));

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemsHistory/findBy")
                    .queryParam("stock_item_id", stockItem.getId())
                    .queryParam("from", fromStr)
                    .queryParam("to", toStr)
                    .queryParam("pageSize", 2)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItemHistory>> firstPage = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            String tailUrl = UriComponentsBuilder.fromPath("/api/v1/stockItemsHistory/findBy")
                    .queryParam("stock_item_id", stockItem.getId())
                    .queryParam("from", fromStr)
                    .queryParam("to", toStr)
                    .queryParam("pageSize", 1)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItemHistory>> tailPage = restClient.exchange(
                    tailUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(firstPage.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(tailPage.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody()).isNotNull().hasSizeLessThanOrEqualTo(2);
            assertThat(tailPage.getBody()).isNotNull().hasSizeLessThanOrEqualTo(1);

            List<StockItemHistory> combined = new ArrayList<>(firstPage.getBody());
            combined.addAll(tailPage.getBody());

            assertThat(combined)
                    .allMatch(history -> history.getStockItemId().equals(stockItem.getId()));
            assertThat(combined)
                    .allMatch(history ->
                            (history.getOldExpiration() == null || !history.getOldExpiration().isBefore(d10)) &&
                                    (history.getNewExpiration() == null || !history.getNewExpiration().isAfter(d30))
                    );
        }

        @Test
        @DisplayName("findBy_success_allWhenParamsNull")
        void findBy_success_allWhenParamsNull() {
            StockItemHistory saved = stockItemHistoryRepository.save(
                    StockItemHistory.builder()
                            .stockItemId(stockItem.getId())
                            .currentProductPrice(product.getPrice())
                            .loggedAt(LocalDateTime.now(Clock.systemUTC()))
                            .build()
            );

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemsHistory/findBy")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItemHistory>> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();
            assertThat(response.getBody()
                    .stream()
                    .map(StockItemHistory::getId))
                    .contains(saved.getId());
        }

        @Test
        @DisplayName("findBy_fails_invalidPagination_returns4xx")
        void findBy_fails_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/stockItemsHistory/findBy")
                    .queryParam("pageSize", 0)
                    .queryParam("page", -1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders),
                    String.class
            );

            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }
}
