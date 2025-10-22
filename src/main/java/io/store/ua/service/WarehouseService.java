package io.store.ua.service;

import io.store.ua.entity.RegularUser;
import io.store.ua.entity.Warehouse;
import io.store.ua.enums.Role;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.WarehouseDTO;
import io.store.ua.repository.WarehouseRepository;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Validated
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;
    private final FieldValidator fieldValidator;

    @PreAuthorize("hasAnyAuthority('OWNER')")
    public List<Warehouse> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                   @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return warehouseRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public Warehouse findByCode(@NotBlank(message = "Warehouse code can't be blank") String code) {
        return warehouseRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Warehouse with code '%s' was not found".formatted(code)));
    }

    @PreAuthorize("hasAnyAuthority('OWNER')")
    public Warehouse save(WarehouseDTO warehouseDTO) {
        RegularUser regularUser =
                RegularUserService.getCurrentlyAuthenticatedUser()
                        .filter(user -> user.getRole().equals(Role.OWNER))
                        .orElseThrow(() -> new BusinessException("Action is allowed for %s only".formatted(Role.OWNER.name())));

        String code = CodeGenerator.WarehouseCodeGenerator.generate(warehouseDTO);

        Optional<Warehouse> warehouseOptional = warehouseRepository.findByCode(code);

        return warehouseOptional.orElseGet(() -> warehouseRepository.save(Warehouse.builder().code(code)
                .name(warehouseDTO.getName())
                .address(warehouseDTO.getAddress())
                .workingHours(warehouseDTO.getWorkingHours())
                .phones(warehouseDTO.getPhones())
                .managerId(regularUser.getId())
                .isActive(warehouseDTO.getIsActive()).build()));
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public Warehouse update(WarehouseDTO warehouseDTO) {
        RegularUser regularUser =
                RegularUserService.getCurrentlyAuthenticatedUser()
                        .filter(user -> List.of(Role.OWNER, Role.MANAGER).contains(user.getRole()))
                        .orElseThrow(() -> new BusinessException("Action is allowed for [%s] only".formatted(List.of(Role.OWNER, Role.MANAGER))));

        fieldValidator.validateObject(warehouseDTO, WarehouseDTO.Fields.code, true);
        Warehouse warehouse =
                warehouseRepository.findByCode(warehouseDTO.getCode()).orElseThrow(() ->
                        new NotFoundException("Warehouse with code %s was not found".formatted(warehouseDTO.getCode())));

        if (warehouseDTO.getName() != null) {
            fieldValidator.validate(warehouseDTO, WarehouseDTO.Fields.name, true);
            warehouse.setName(warehouseDTO.getName());
        }
        if (warehouseDTO.getAddress() != null) {
            fieldValidator.validateObject(warehouseDTO, WarehouseDTO.Fields.address, true);
            warehouse.setAddress(warehouseDTO.getAddress());
        }
        if (warehouseDTO.getWorkingHours() != null) {
            fieldValidator.validateObject(warehouseDTO, WarehouseDTO.Fields.workingHours, true);
            warehouse.setWorkingHours(warehouseDTO.getWorkingHours());
        }
        if (warehouseDTO.getPhones() != null) {
            fieldValidator.validateObject(warehouseDTO, WarehouseDTO.Fields.phones, true);
            warehouse.setPhones(warehouseDTO.getPhones());
        }
        if (warehouseDTO.getIsActive() != null) {
            fieldValidator.validate(warehouseDTO, WarehouseDTO.Fields.isActive, true);
            warehouse.setIsActive(warehouseDTO.getIsActive());
        }

        if (warehouseDTO.getManagerId() != null && regularUser.getRole().equals(Role.OWNER)) {
            warehouse.setManagerId(warehouseDTO.getManagerId());
        }

        return warehouseRepository.save(warehouse);
    }

    @PreAuthorize("hasAnyAuthority('OWNER')")
    public Warehouse toggleState(@NotBlank(message = "Warehouse code can't be blank") String code) {
        Warehouse warehouse = warehouseRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException(("Warehouse with code: %s was not found").formatted(code)));

        warehouse.setIsActive(!warehouse.getIsActive());

        return warehouseRepository.save(warehouse);
    }
}
