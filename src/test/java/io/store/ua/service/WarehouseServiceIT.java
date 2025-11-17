package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.User;
import io.store.ua.entity.Warehouse;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.data.Address;
import io.store.ua.models.data.WorkingHours;
import io.store.ua.models.dto.WarehouseDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WarehouseServiceIT extends AbstractIT {
    @Autowired
    private WarehouseService warehouseService;

    @Nested
    @DisplayName("save(warehouse: WarehouseDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: creates a new Warehouse when absent")
        void save_success() {
            var warehouseDTO = buildWarehouseDTO();

            Warehouse result = warehouseService.save(warehouseDTO);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo(warehouseDTO.getName());
            assertThat(result.getIsActive()).isTrue();
            assertThat(warehouseRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("save_fail_whenNameMissing_forNewWarehouse")
        void save_fail_whenNameMissing_forNewWarehouse() {
            var warehouseDTO = buildWarehouseDTO();
            warehouseDTO.setName(null);

            assertThatThrownBy(() -> warehouseService.save(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("save_fail_whenIsActiveMissing_forNewWarehouse")
        void save_fail_whenIsActiveMissing_forNewWarehouse() {
            var warehouseDTO = buildWarehouseDTO();
            warehouseDTO.setIsActive(null);

            assertThatThrownBy(() -> warehouseService.save(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("save_fail_whenAddressMissing_forNewWarehouse")
        void save_fail_whenAddressMissing_forNewWarehouse() {
            var warehouseDTO = buildWarehouseDTO();
            warehouseDTO.setAddress(null);

            assertThatThrownBy(() -> warehouseService.save(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("save_fail_whenWorkingHoursMissing_forNewWarehouse")
        void save_fail_whenWorkingHoursMissing_forNewWarehouse() {
            var warehouseDTO = buildWarehouseDTO();
            warehouseDTO.setWorkingHours(null);

            assertThatThrownBy(() -> warehouseService.save(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("save_fail_whenPhonesInvalid")
        void save_fail_whenPhonesInvalid() {
            var warehouseDTO = buildWarehouseDTO();
            warehouseDTO.setPhones(new ArrayList<>() {{
                add(null);
            }});

            assertThatThrownBy(() -> warehouseService.save(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("update(warehouse: WarehouseDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success: applies partial field updates and persists")
        void update_success_partialFields() {
            Warehouse result = warehouseService.save(buildWarehouseDTO());

            Address newAddress = Address.builder()
                    .country("UA")
                    .state("Kyiv")
                    .city("Kyiv")
                    .street("B. Khmelnytskoho")
                    .building("10")
                    .postalCode("01030")
                    .latitude(new BigDecimal("50.4502"))
                    .longitude(new BigDecimal("30.5235"))
                    .build();

            WorkingHours workingHours = WorkingHours.builder()
                    .timezone("UTC")
                    .days(List.of(
                            WorkingHours.DayHours.builder()
                                    .day(DayOfWeek.TUESDAY)
                                    .open(List.of(
                                            WorkingHours.TimeRange.builder()
                                                    .from(LocalTime.of(10, 0))
                                                    .to(LocalTime.of(19, 0))
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            var warehouseDTO = WarehouseDTO.builder()
                    .code(result.getCode())
                    .name(RandomStringUtils.secure().nextAlphanumeric(5))
                    .address(newAddress)
                    .workingHours(workingHours)
                    .phones(List.of("+380441234567", "+380501112233"))
                    .isActive(false)
                    .build();

            Warehouse updated = warehouseService.update(warehouseDTO);

            assertThat(updated.getId()).isEqualTo(result.getId());
            assertThat(updated.getName()).isEqualTo(warehouseDTO.getName());
            assertThat(updated.getAddress()).isEqualTo(warehouseDTO.getAddress());
            assertThat(updated.getWorkingHours()).isEqualTo(warehouseDTO.getWorkingHours());
            assertThat(updated.getPhones()).containsExactlyElementsOf(warehouseDTO.getPhones());
            assertThat(updated.getIsActive()).isFalse();

            Warehouse reloaded = warehouseRepository.findByCode(result.getCode()).orElseThrow();

            assertThat(reloaded.getName()).isEqualTo(warehouseDTO.getName());
            assertThat(reloaded.getAddress()).isEqualTo(warehouseDTO.getAddress());
            assertThat(reloaded.getWorkingHours()).isEqualTo(warehouseDTO.getWorkingHours());
            assertThat(reloaded.getPhones()).containsExactlyElementsOf(warehouseDTO.getPhones());
            assertThat(reloaded.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("update_success: reassignWarehouseManagement=true sets current user as manager")
        void update_success_reassignManagement() {
            Warehouse result = warehouseService.save(buildWarehouseDTO());
            var extraUser = userRepository.save(User.builder()
                    .username(RandomStringUtils.secure().nextAlphanumeric(64))
                    .password(passwordEncoder.encode(RandomStringUtils.secure().nextAlphanumeric(64)))
                    .email(RandomStringUtils.secure().nextAlphanumeric(32))
                    .role(UserRole.OWNER)
                    .status(UserStatus.ACTIVE)
                    .timezone("UTC")
                    .build());

            var warehouseDTO = WarehouseDTO.builder()
                    .code(result.getCode())
                    .managerId(extraUser.getId())
                    .build();

            Warehouse updated = warehouseService.update(warehouseDTO);

            assertThat(updated.getManagerId()).isEqualTo(extraUser.getId());
            Warehouse reloaded = warehouseRepository.findByCode(result.getCode()).orElseThrow();
            assertThat(reloaded.getManagerId()).isEqualTo(extraUser.getId());
        }

        @Test
        @DisplayName("update_fail_whenWarehouseInvalid: throws ValidationException if warehouse is invalid")
        void update_fail_whenWarehouseInvalid() {
            assertThatThrownBy(() -> warehouseService.update(null))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fail_whenNotFound: throws NotFoundException if code not found")
        void update_fail_whenNotFound() {
            var warehouseDTO = WarehouseDTO.builder()
                    .code(RandomStringUtils.secure().nextAlphanumeric(24))
                    .name("Whatever")
                    .build();

            assertThatThrownBy(() -> warehouseService.update(warehouseDTO))
                    .isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest(name = "update_fail_whenInvalidCode: ''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("update_fail_whenInvalidCode: code must be present and non-blank")
        void update_fail_whenInvalidCode(String code) {
            WarehouseDTO warehouseDTO = WarehouseDTO.builder()
                    .code(code)
                    .name("X")
                    .build();

            assertThatThrownBy(() -> warehouseService.update(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest(name = "update_fail_whenInvalidName: ''{0}''")
        @ValueSource(strings = {" ", "", "\t", "\n"})
        @DisplayName("update_fail_whenInvalidName: name must be valid when provided")
        void update_fail_whenInvalidName(String badName) {
            Warehouse result = warehouseService.save(buildWarehouseDTO());

            WarehouseDTO warehouseDTO = WarehouseDTO.builder()
                    .code(result.getCode())
                    .name(badName)
                    .build();

            assertThatThrownBy(() -> warehouseService.update(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fail_whenInvalidPhones: phones must be valid when provided")
        void update_fail_whenInvalidPhones() {
            Warehouse result = warehouseService.save(buildWarehouseDTO());

            WarehouseDTO warehouseDTO = WarehouseDTO.builder()
                    .code(result.getCode())
                    .phones(List.of("not-a-phone", "123"))
                    .build();

            assertThatThrownBy(() -> warehouseService.update(warehouseDTO))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("toggleState(code: string)")
    class ToggleStateTests {
        @Test
        @DisplayName("toggleState_success: flips isActive and persists")
        void toggleState_success() {
            Warehouse warehouse = generateWarehouse();

            Warehouse fetchWarehouse = warehouseService.toggleState(warehouse.getCode());

            assertThat(fetchWarehouse.getIsActive()).isEqualTo(!warehouse.getIsActive());
            Warehouse reloaded = warehouseRepository.findByCode(warehouse.getCode()).orElseThrow();
            assertThat(reloaded.getIsActive()).isEqualTo(fetchWarehouse.getIsActive());
        }

        @Test
        @DisplayName("toggleState_fail_whenNotFound: throws NotFoundException if warehouse is not found by code")
        void toggleState_fail_whenNotFound() {
            assertThatThrownBy(() -> warehouseService.toggleState(RandomStringUtils.secure().nextAlphanumeric(33333)))
                    .isInstanceOf(NotFoundException.class);
        }

        @ParameterizedTest(name = "toggleState_fail_whenInvalidCode: ''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void toggleState_fail_whenInvalidCode(String code) {
            assertThatThrownBy(() -> warehouseService.toggleState(code))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
