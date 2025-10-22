package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.RegularUser;
import io.store.ua.entity.StockItemGroup;
import io.store.ua.enums.Role;
import io.store.ua.enums.Status;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemGroupDTO;
import io.store.ua.utility.CodeGenerator;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StockItemGroupServiceIT extends AbstractIT {
    @Autowired
    private StockItemGroupService stockItemGroupService;

    @BeforeEach
    void setUp() {
        var user = RegularUser.builder()
                .username(RandomStringUtils.secure().nextAlphanumeric(333))
                .role(Role.OWNER)
                .status(Status.ACTIVE)
                .build();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
    }

    private StockItemGroupDTO generateStockItemGroupDTO() {
        return StockItemGroupDTO.builder()
                .name(RandomStringUtils.secure().nextAlphanumeric(33))
                .code(CodeGenerator.StockCodeGenerator.generate())
                .isActive(true)
                .build();
    }

    private List<StockItemGroup> generateGroups(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(
                        ignore ->
                                StockItemGroup.builder()
                                        .code(RandomStringUtils.secure().nextAlphanumeric(330))
                                        .name(RandomStringUtils.secure().nextAlphanumeric(33))
                                        .isActive(true)
                                        .build())
                .toList();
    }

    @Nested
    @DisplayName("save(code: string, name: string, warehouseCode: string)")
    class SaveTests {
        private static Stream<StockItemGroupDTO> invalidGroups() {
            return Stream.of(
                    StockItemGroupDTO.builder().code(null).name(null).build(),
                    StockItemGroupDTO.builder().code("").name("").build(),
                    StockItemGroupDTO.builder().code("AAA").name("").build()
            );
        }

        @Test
        @DisplayName("save_success: creates a new stock item group")
        @Transactional
        void save_success() {
            var stockItemGroupDTO = generateStockItemGroupDTO();
            stockItemGroupDTO.setCode(null);

            StockItemGroup group = stockItemGroupService.save(stockItemGroupDTO);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getCode()).isNotNull();
            assertThat(group.getName()).isEqualTo(stockItemGroupDTO.getName());
            assertThat(group.getIsActive()).isEqualTo(stockItemGroupDTO.getIsActive());

            assertThat(stockItemGroupRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("save_update: updates entity when StockItemGroupDTO has a code linking to existing row")
        void save_update() {
            var stockItemGroupDTO = generateStockItemGroupDTO();
            stockItemGroupDTO.setCode(null);

            StockItemGroup group = stockItemGroupService.save(stockItemGroupDTO);

            assertThat(group.getId()).isNotNull();
            assertThat(group.getCode()).isNotNull();
            assertThat(group.getName()).isEqualTo(stockItemGroupDTO.getName());
            assertThat(group.getIsActive()).isEqualTo(stockItemGroupDTO.getIsActive());

            assertThat(stockItemGroupRepository.count()).isEqualTo(1);

            stockItemGroupDTO.setCode(group.getCode());
            stockItemGroupDTO.setName(RandomStringUtils.secure().nextAlphanumeric(33));
            stockItemGroupDTO.setIsActive(!stockItemGroupDTO.getIsActive());

            StockItemGroup updatedGroup = stockItemGroupService.save(stockItemGroupDTO);

            assertThat(updatedGroup.getId()).isEqualTo(group.getId());
            assertThat(updatedGroup.getCode()).isEqualTo(group.getCode());
            assertThat(updatedGroup.getName()).isEqualTo(stockItemGroupDTO.getName());
            assertThat(updatedGroup.getIsActive()).isEqualTo(stockItemGroupDTO.getIsActive());
            assertThat(group.getName()).isNotEqualTo(updatedGroup.getName());
            assertThat(group.getIsActive()).isNotEqualTo(updatedGroup.getIsActive());

            assertThat(stockItemGroupRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("save_idempotence: returns existing group when group exists by code")
        void save_idempotence() {
            StockItemGroup group = stockItemGroupRepository.save(generateGroups(1).getFirst());

            StockItemGroup result = stockItemGroupService.save(StockItemGroupDTO.builder().code(group.getCode()).build());

            assertThat(result.getId()).isEqualTo(group.getId());

            assertThat(stockItemGroupRepository.count()).isEqualTo(1);
        }

        @ParameterizedTest
        @DisplayName("save_fail_whenInvalidStockItemGroupDTO: throws an exception when invalid StockItemGroupDTO")
        @MethodSource("invalidGroups")
        void save_fail_whenInvalidStockItemGroupDTO(StockItemGroupDTO stockItemGroupDTO) {
            assertThatThrownBy(() -> stockItemGroupService.save(stockItemGroupDTO));
        }


        @Test
        @DisplayName("save_update: updates entity when StockItemGroupDTO has a code linking to existing row")
        void save_update_fail_whenGroupWasNotFoundByCode() {
            var stockItemGroupDTO = generateStockItemGroupDTO();

            assertThatThrownBy(() -> stockItemGroupService.save(stockItemGroupDTO))
                    .isInstanceOf(NotFoundException.class);

            assertThat(stockItemGroupRepository.count()).isEqualTo(0);
        }

    }

    @Nested
    @DisplayName("findAll(pageSize: int, page: int)")
    class FindAllTests {
        @ParameterizedTest(name = "findAll_success: pageSize={0} (int), page={1} (int)")
        @CsvSource({"1, 1", "3, 2", "5, 3"})
        @Transactional
        void findAll_success(int pageSize, int page) {
            var groups = stockItemGroupRepository.saveAll(generateGroups(pageSize * page));

            var result = stockItemGroupService.findAll(pageSize, page);

            assertThat(result).hasSize(pageSize);
            assertThat(result)
                    .extracting(StockItemGroup::getCode)
                    .containsExactlyInAnyOrderElementsOf(
                            groups.subList((page - 1) * pageSize, page * pageSize).stream()
                                    .map(StockItemGroup::getCode)
                                    .toList());
        }

        @ParameterizedTest(name = "findAll_fail_whenPageSizeIsInvalid: pageSize={0} (int)")
        @ValueSource(ints = {0, -1, -10})
        void findAll_fail_whenPageSizeIsInvalid(int pageSize) {
            assertThatThrownBy(() -> stockItemGroupService.findAll(pageSize, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest(name = "findAll_fail_whenPageIsInvalid: page={0} (int)")
        @ValueSource(ints = {0, -1, -7})
        void findAll_fail_whenPageIsInvalid(int page) {
            assertThatThrownBy(() -> stockItemGroupService.findAll(1, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("findByCode(code: string)")
    class FindByCodeTests {
        @Test
        @DisplayName("findByCode_success: returns existing group")
        void findByCode_success() {
            StockItemGroup group = generateGroups(1).getFirst();

            stockItemGroupRepository.save(group);

            StockItemGroup found = stockItemGroupService.findByCode(group.getCode());

            assertThat(found.getCode()).isEqualTo(group.getCode());
        }

        @Test
        @DisplayName("findByCode_fail_whenNotFound: throws NotFoundException")
        void findByCode_fail_whenNotFound() {
            assertThatThrownBy(
                    () ->
                            stockItemGroupService.findByCode(RandomStringUtils.secure().nextAlphanumeric(10)))
                    .isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest(name = "findByCode_fail_whenCodeIsInvalid: code=''{0}'' (string)")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void findByCode_fail_whenCodeIsInvalid(String code) {
            assertThatThrownBy(() -> stockItemGroupService.findByCode(code))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("toggleState(code: string)")
    class ToggleStateTests {
        @Test
        @DisplayName("toggleState_success: flips isActive and persists")
        void toggleState_success() {
            StockItemGroup group = stockItemGroupRepository.save(generateGroups(1).getFirst());

            StockItemGroup toggled = stockItemGroupService.toggleState(group.getCode());

            assertEquals(group.getIsActive(), !toggled.getIsActive());

            StockItemGroup reloaded = stockItemGroupRepository.findByCode(group.getCode()).orElseThrow();

            assertThat(reloaded.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("toggleState_fail_whenNotFound: throws NotFoundException")
        void toggleState_fail_whenNotFound() {
            assertThatThrownBy(
                    () ->
                            stockItemGroupService.toggleState(
                                    RandomStringUtils.secure().nextAlphanumeric(10)))
                    .isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest(name = "toggleState_fail_whenCodeIsInvalid: code=''{0}'' (string)")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void toggleState_fail_whenCodeIsInvalid(String code) {
            assertThatThrownBy(() -> stockItemGroupService.toggleState(code))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
