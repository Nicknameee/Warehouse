package io.store.ua.controllers;

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

    private HttpHeaders generateHeaders(HttpHeaders base) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(base);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
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
        @DisplayName("findBy_success_allFiltersApplied")
        void findBy_success_allFiltersApplied() {
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
                    .queryParam("warehouseId", warehouseA.getId())
                    .queryParam("productId", productA.getId())
                    .queryParam("stockItemGroupId", groupA.getId())
                    .queryParam("status", StockItemStatus.AVAILABLE.name())
                    .queryParam("storageSectionId", sectionA0.getId())
                    .queryParam("isItemActive", true)
                    .queryParam("isItemGroupActive", true)
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<StockItem>> response = restClient.exchange(
                    url,
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
    }

    @Nested
    @DisplayName("POST /api/v1/stockItems")
    class CreateTests {
        @Test
        @DisplayName("create_success_allFieldsAndStatusComputed")
        void create_success_allFieldsAndStatusComputed() {
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

            ResponseEntity<StockItem> response = restClient.exchange(
                    "/api/v1/stockItems",
                    HttpMethod.POST,
                    new HttpEntity<>(stockItemDTO, generateHeaders(authenticationHeaders)),
                    StockItem.class
            );

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
            assertThat(stockItem.getIsActive())
                    .isTrue();
            assertThat(stockItem.getStorageSectionId())
                    .isEqualTo(sectionA0.getId());
            assertThat(stockItem.getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);

            StockItem persisted = stockItemRepository.findById(stockItem.getId())
                    .orElseThrow();

            assertThat(persisted.getWarehouseId())
                    .isEqualTo(stockItemDTO.getWarehouseId());
            assertThat(persisted.getStatus())
                    .isEqualTo(StockItemStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/stockItems")
    class UpdateTests {
        @Test
        @DisplayName("update_success_updatesFieldsAndRecomputesStatus")
        void update_success_updatesFieldsAndRecomputesStatus() {
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

            ResponseEntity<StockItem> response = restClient.exchange(
                    "/api/v1/stockItems",
                    HttpMethod.PUT,
                    new HttpEntity<>(stockItemDTO, generateHeaders(authenticationHeaders)),
                    StockItem.class
            );

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

            StockItem persisted = stockItemRepository.findById(stockItem.getId())
                    .orElseThrow();

            assertThat(persisted.getStatus())
                    .isEqualTo(StockItemStatus.OUT_OF_STOCK);
            assertThat(persisted.getIsActive())
                    .isFalse();
            assertThat(persisted.getStorageSectionId())
                    .isEqualTo(sectionA0.getId());
        }
    }
}
