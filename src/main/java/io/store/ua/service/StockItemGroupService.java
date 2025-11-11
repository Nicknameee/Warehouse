package io.store.ua.service;

import io.store.ua.entity.StockItemGroup;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemGroupDTO;
import io.store.ua.repository.StockItemGroupRepository;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Validated
public class StockItemGroupService {
    private final StockItemGroupRepository stockItemGroupRepository;
    private final FieldValidator fieldValidator;

    public List<StockItemGroup> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                        @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return stockItemGroupRepository
                .findAll(Pageable.ofSize(pageSize).withPage(page - 1))
                .getContent();
    }

    public StockItemGroup findByCode(@NotBlank(message = "Stock group code can not be blank") String code) {
        return stockItemGroupRepository
                .findByCode(code)
                .orElseThrow(
                        () -> new NotFoundException("Stock group with code: %s was not found".formatted(code)));
    }

    public StockItemGroup save(@NotNull(message = "Stock item group can't be null") StockItemGroupDTO stockItemGroupDTO) {
        if (stockItemGroupDTO.getCode() == null) {
            if (stockItemGroupDTO.getName() == null) {
                throw new ValidationException("Stock group can't be created without name");
            } else {
                fieldValidator.validate(stockItemGroupDTO, StockItemGroupDTO.Fields.name, true);
                Optional<StockItemGroup> stockItemGroup = stockItemGroupRepository.findByName(stockItemGroupDTO.getName());

                return stockItemGroup.orElseGet(
                        () ->
                                stockItemGroupRepository.save(
                                        StockItemGroup.builder()
                                                .code(CodeGenerator.StockCodeGenerator.generate())
                                                .name(stockItemGroupDTO.getName())
                                                .isActive(stockItemGroupDTO.getIsActive() != null && stockItemGroupDTO.getIsActive())
                                                .build()));
            }
        } else {
            fieldValidator.validate(stockItemGroupDTO, StockItemGroupDTO.Fields.code, true);
            Optional<StockItemGroup> stockItemGroup = stockItemGroupRepository.findByCode(stockItemGroupDTO.getCode());

            if (stockItemGroup.isPresent()) {
                StockItemGroup group = stockItemGroup.get();

                if (stockItemGroupDTO.getName() != null) {
                    fieldValidator.validate(stockItemGroupDTO, StockItemGroupDTO.Fields.name, true);
                    group.setName(stockItemGroupDTO.getName());
                }

                if (stockItemGroupDTO.getIsActive() != null) {
                    group.setIsActive(stockItemGroupDTO.getIsActive());
                }

                return stockItemGroupRepository.save(group);
            }

            throw new NotFoundException("Stock group with code: %s was not found".formatted(stockItemGroupDTO.getCode()));
        }
    }

    public StockItemGroup update(@NotNull(message = "Group ID can't be null")
                                 @Min(value = 1, message = "Group ID can't be less than 1")
                                 Long groupId,
                                 String groupName,
                                 Boolean isActive) {
        StockItemGroup stockItemGroup =
                stockItemGroupRepository
                        .findById(groupId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Stock group with ID '%s' was not found".formatted(groupId)));

        if (!StringUtils.isBlank(groupName)) {
            stockItemGroup.setName(groupName);
        }

        if (isActive != null) {
            stockItemGroup.setIsActive(isActive);
        }

        return stockItemGroupRepository.save(stockItemGroup);
    }
}
