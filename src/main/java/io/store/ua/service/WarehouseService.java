package io.store.ua.service;

import io.store.ua.entity.User;
import io.store.ua.entity.Warehouse;
import io.store.ua.enums.UserRole;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.WarehouseDTO;
import io.store.ua.repository.WarehouseRepository;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Validated
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;

    public List<Warehouse> findBy(String codePrefix,
                                  String namePrefix,
                                  Long managerId,
                                  Boolean isActive,
                                  @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                  @Min(value = 1, message = "A number of page can't be less than one") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Warehouse> criteriaQuery = criteriaBuilder.createQuery(Warehouse.class);
        Root<Warehouse> root = criteriaQuery.from(Warehouse.class);

        List<Predicate> predicates = new ArrayList<>();

        if (codePrefix != null && !codePrefix.isEmpty()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Warehouse.Fields.code)), codePrefix.toLowerCase() + "%"));
        }

        if (namePrefix != null && !namePrefix.isEmpty()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Warehouse.Fields.name)), namePrefix.toLowerCase() + "%"));
        }

        if (managerId != null) {
            predicates.add(criteriaBuilder.equal(root.get(Warehouse.Fields.managerId), managerId));
        }

        if (isActive != null) {
            predicates.add(criteriaBuilder.equal(root.get(Warehouse.Fields.isActive), isActive));
        }

        criteriaQuery.where(predicates.toArray(new Predicate[0]));
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get(Warehouse.Fields.id)));

        return entityManager
                .createQuery(criteriaQuery)
                .setFirstResult(pageSize * (page - 1))
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Warehouse save(@NotNull(message = "Warehouse can't be null") WarehouseDTO warehouseDTO) {
        fieldValidator.validateObjects(warehouseDTO, true,
                WarehouseDTO.Fields.address,
                WarehouseDTO.Fields.workingHours);

        String code = CodeGenerator.WarehouseCodeGenerator.generate(warehouseDTO);

        Optional<Warehouse> warehouseOptional = warehouseRepository.findByCode(code);

        if (warehouseOptional.isEmpty()) {
            fieldValidator.validate(warehouseDTO, true,
                    WarehouseDTO.Fields.name,
                    WarehouseDTO.Fields.isActive);
            fieldValidator.validateObjects(warehouseDTO, false, WarehouseDTO.Fields.phones);
            fieldValidator.validate(warehouseDTO, WarehouseDTO.Fields.managerId, false);
        }

        return warehouseOptional.orElseGet(() -> warehouseRepository.save(Warehouse.builder().code(code)
                .name(warehouseDTO.getName())
                .address(warehouseDTO.getAddress())
                .workingHours(warehouseDTO.getWorkingHours())
                .phones(warehouseDTO.getPhones())
                .managerId(warehouseDTO.getManagerId() == null ? UserService.getCurrentlyAuthenticatedUserID() : warehouseDTO.getManagerId())
                .isActive(warehouseDTO.getIsActive()).build()));
    }

    public Warehouse update(@NotNull(message = "Warehouse can't be null") WarehouseDTO warehouseDTO) {
        User user =
                UserService.getCurrentlyAuthenticatedUser()
                        .filter(authentication -> List.of(UserRole.OWNER, UserRole.MANAGER).contains(authentication.getRole()))
                        .orElseThrow(() -> new BusinessException("Action is allowed for [%s] only".formatted(List.of(UserRole.OWNER, UserRole.MANAGER))));

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

        if (warehouseDTO.getManagerId() != null) {
            if (user.getRole().equals(UserRole.OWNER)) {
                warehouse.setManagerId(warehouseDTO.getManagerId());
            } else {
                throw new BusinessException("Updating manager for warehouse is allowed for %s only".formatted(UserRole.OWNER.name()));
            }
        }

        return warehouseRepository.save(warehouse);
    }

    public Warehouse toggleState(@NotBlank(message = "Warehouse code can't be blank") String code) {
        Warehouse warehouse = warehouseRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException(("Warehouse with code: %s was not found").formatted(code)));

        warehouse.setIsActive(!warehouse.getIsActive());

        return warehouseRepository.save(warehouse);
    }
}
