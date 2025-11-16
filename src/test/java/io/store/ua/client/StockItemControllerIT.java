package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.*;
import io.store.ua.enums.StockItemStatus;
import io.store.ua.models.dto.StockItemDTO;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockItemControllerIT extends AbstractIT {
    private HttpHeaders authenticationHeaders;

    private Warehouse warehouseA;
    private Warehouse warehouseB;
    private Product productA;
    private Product productB;
    private StockItemGroup groupA;
    private StockItemGroup groupB;
    private StorageSection sectionA0;
    private StorageSection sectionA1;
    private StorageSection sectionB0;

    @BeforeAll
    void setupAuthentication() {
        authenticationHeaders = generateAuthenticationHeaders();
    }

    @BeforeEach
    void setUp() {
        groupA = generateStockItemGroup(true);
        groupB = generateStockItemGroup(true);
        productA = generateProduct();
        productB = generateProduct();
        warehouseA = generateWarehouse();
        warehouseB = generateWarehouse();
        sectionA0 = generateStorageSection(warehouseA.getId());
        sectionA1 = generateStorageSection(warehouseA.getId());
        sectionB0 = generateStorageSection(warehouseB.getId());
    }

    private StockItem generateStockItem(Product product,
                                        StockItemGroup group,
                                        Warehouse warehouse,
                                        boolean isActive,
                                        int quantity,
                                        Long sectionId) {
        return stockItemRepository.save(StockItem.builder()
                .productId(product.getId())
                .stockItemGroupId(group.getId())
                .warehouseId(warehouse.getId())
                .expiryDate(LocalDate.now().plusDays(RandomUtils.secure().randomInt(15, 120)))
                .availableQuantity(BigInteger.valueOf(quantity))
                .status(quantity > 0
                        ? StockItemStatus.AVAILABLE : StockItemStatus.OUT_OF_STOCK)
                .isActive(isActive)
                .storageSectionId(sectionId)
                .build());
    }

    @Nested
    @DisplayName("GET /api/v1/stockItems/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success")
        void findBy_success() {
            StockItem shouldMatch = generateStockItem(productA,
                    groupA,
                    warehouseA,
                    true,
                    7,
                    sectionA0.getId());
            StockItem otherWarehouse = generateStockItem(productA,
                    groupA,
                    warehouseB,
                    true,
                    7,
                    sectionB0.getId());
            StockItem otherProduct = generateStockItem(productB,
                    groupA,
                    warehouseA,
                    true,
                    7,
                    sectionA0.getId());
            StockItem otherGroup = generateStockItem(productB,
                    groupB,
                    warehouseB,
                    true,
                    7,
                    sectionB0.getId());
            Product newProduct = generateProduct();
            StockItem inactiveItem = generateStockItem(newProduct,
                    groupA,
                    warehouseA,
                    false,
                    7,
                    sectionA0.getId());

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItems/findBy")
                    .queryParam("warehouse_id", warehouseA.getId())
                    .queryParam("product_id", productA.getId())
                    .queryParam("stock_item_group_id", groupA.getId())
                    .queryParam("status", StockItemStatus.AVAILABLE.name())
                    .queryParam("storage_section_id", sectionA0.getId())
                    .queryParam("is_item_active", true)
                    .queryParam("is_item_group_active", true)
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItem>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(StockItem::getId)
                    .contains(shouldMatch.getId())
                    .doesNotContain(otherWarehouse.getId(),
                            otherProduct.getId(),
                            otherGroup.getId(),
                            inactiveItem.getId());
        }

        @Test
        @DisplayName("findBy_success_onlyActiveItemsAndGroups")
        void findBy_success_onlyActiveItemsAndGroups() {
            StockItem activeInGroup = generateStockItem(productA,
                    groupA,
                    warehouseA,
                    true,
                    5,
                    null);
            StockItem inactiveItem = generateStockItem(productB,
                    groupA,
                    warehouseA,
                    false,
                    5,
                    null);

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItems/findBy")
                    .queryParam("warehouse_id", warehouseA.getId())
                    .queryParam("product_id", productA.getId())
                    .queryParam("stock_item_group_id", groupA.getId())
                    .queryParam("status", StockItemStatus.AVAILABLE.name())
                    .queryParam("is_item_active", true)
                    .queryParam("is_item_group_active", true)
                    .queryParam("pageSize", 100)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItem>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(StockItem::getId)
                    .contains(activeInGroup.getId())
                    .doesNotContain(inactiveItem.getId());
        }

        @Test
        @DisplayName("findBy_success_filterByStorageSectionIds")
        void findBy_success_filterByStorageSectionIds() {
            StockItem shouldMatch = generateStockItem(productA,
                    groupA,
                    warehouseA,
                    true,
                    3,
                    sectionA1.getId());
            StockItem otherSection = generateStockItem(productB,
                    groupA,
                    warehouseA,
                    true,
                    3,
                    sectionA0.getId());

            String url = UriComponentsBuilder.fromPath("/api/v1/stockItems/findBy")
                    .queryParam("product_id", productA.getId())
                    .queryParam("stock_item_group_id", groupA.getId())
                    .queryParam("status", StockItemStatus.AVAILABLE.name())
                    .queryParam("storage_section_id", sectionA1.getId())
                    .queryParam("is_item_active", true)
                    .queryParam("is_item_group_active", true)
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItem>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .extracting(StockItem::getId)
                    .containsExactly(shouldMatch.getId());
            assertThat(response.getBody())
                    .allSatisfy(stockItem -> assertThat(stockItem.getStorageSectionId())
                            .isEqualTo(sectionA1.getId()));
            assertThat(response.getBody())
                    .extracting(StockItem::getId)
                    .doesNotContain(otherSection.getId());
        }

        @Test
        @DisplayName("findBy_fails_invalidStatusString_returns4xx")
        void findBy_fails_invalidStatusString_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/stockItems/findBy")
                    .queryParam("status", "WRONG_STATUS")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }

        @Test
        @DisplayName("findBy_fails_invalidPagination_returns4xx")
        void findBy_fails_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/stockItems/findBy")
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
    @DisplayName("POST /api/v1/stockItems")
    class CreateTests {
        @Test
        @DisplayName("create_success_createsAndComputesStatus")
        void create_success_createsAndComputesStatus() {
            LocalDate expiration = LocalDate.now().plusDays(30);

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(groupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(expiration)
                    .availableQuantity(BigInteger.TEN)
                    .isActive(true)
                    .storageSectionId(sectionA0.getId())
                    .build();

            ResponseEntity<StockItem> response = restClient.exchange("/api/v1/stockItems",
                    HttpMethod.POST,
                    new HttpEntity<>(stockItemDTO, authenticationHeaders),
                    StockItem.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            StockItem stockItem = response.getBody();

            assertThat(stockItem.getProductId())
                    .isEqualTo(stockItemDTO.getProductId());
            assertThat(stockItem.getStockItemGroupId())
                    .isEqualTo(stockItemDTO.getStockItemGroupId());
            assertThat(stockItem.getWarehouseId())
                    .isEqualTo(stockItemDTO.getWarehouseId());
            assertThat(stockItem.getExpiryDate())
                    .isEqualTo(expiration);
            assertThat(stockItem.getAvailableQuantity())
                    .isEqualByComparingTo(stockItemDTO.getAvailableQuantity());
            assertThat(stockItem.getIsActive()).isTrue();
            assertThat(stockItem.getStorageSectionId())
                    .isEqualTo(sectionA0.getId());
            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);

            StockItem stockItemFetch = stockItemRepository.findById(stockItem.getId())
                    .orElseThrow();

            assertThat(stockItemFetch.getWarehouseId())
                    .isEqualTo(stockItemDTO.getWarehouseId());
            assertThat(stockItemFetch.getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
        }

        @Test
        @DisplayName("create_success_statusOutOfStock")
        void create_success_statusOutOfStock() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .productId(productA.getId())
                    .stockItemGroupId(groupA.getId())
                    .warehouseId(warehouseA.getId())
                    .expiryDate(LocalDate.now().plusDays(10))
                    .availableQuantity(BigInteger.ZERO)
                    .isActive(true)
                    .storageSectionId(sectionA1.getId())
                    .build();

            ResponseEntity<StockItem> response = restClient.exchange("/api/v1/stockItems",
                    HttpMethod.POST,
                    new HttpEntity<>(stockItemDTO, authenticationHeaders),
                    StockItem.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            StockItem created = response.getBody();

            assertThat(created.getStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_STOCK);
            assertThat(created.getStorageSectionId())
                    .isEqualTo(sectionA1.getId());
        }

        @Test
        @DisplayName("create_fails_nullPayload_returns4xx")
        void create_fails_nullPayload_returns4xx() {
            ResponseEntity<String> response = restClient.exchange("/api/v1/stockItems",
                    HttpMethod.POST,
                    new HttpEntity<>(null, authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/stockItems")
    class UpdateTests {
        @Test
        @DisplayName("update_success_updatesFields_andRecomputesStatus")
        void update_success_updatesFields_andRecomputesStatus() {
            StockItem stockItem = generateStockItem(productA,
                    groupA,
                    warehouseA,
                    true,
                    5,
                    null);

            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .stockItemId(stockItem.getId())
                    .availableQuantity(BigInteger.ZERO)
                    .isActive(false)
                    .storageSectionId(sectionA0.getId())
                    .build();

            ResponseEntity<StockItem> response = restClient.exchange("/api/v1/stockItems",
                    HttpMethod.PUT,
                    new HttpEntity<>(stockItemDTO, authenticationHeaders),
                    StockItem.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            StockItem stockItemUpdated = response.getBody();

            assertThat(stockItemUpdated.getId())
                    .isEqualTo(stockItem.getId());
            assertThat(stockItemUpdated.getAvailableQuantity())
                    .isEqualByComparingTo(BigInteger.ZERO);
            assertThat(stockItemUpdated.getIsActive())
                    .isFalse();
            assertThat(stockItemUpdated.getStorageSectionId())
                    .isEqualTo(sectionA0.getId());
            assertThat(stockItemUpdated.getStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_STOCK);

            StockItem persisted = stockItemRepository.findById(stockItem.getId()).orElseThrow();

            assertThat(persisted.getStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_STOCK);
            assertThat(persisted.getIsActive())
                    .isFalse();
            assertThat(persisted.getStorageSectionId())
                    .isEqualTo(sectionA0.getId());
        }

        @Test
        @DisplayName("update_fails_missingId_returns4xx")
        void update_fails_missingId_returns4xx() {
            StockItemDTO stockItemDTO = StockItemDTO.builder()
                    .availableQuantity(BigInteger.TEN)
                    .build();

            ResponseEntity<String> response = restClient.exchange("/api/v1/stockItems",
                    HttpMethod.PUT,
                    new HttpEntity<>(stockItemDTO, authenticationHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }
}
