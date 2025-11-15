package io.store.ua.service;

import io.store.ua.entity.StockItemGroup;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.StockItemGroupDTO;
import io.store.ua.repository.StockItemGroupRepository;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Validated
public class StockItemGroupService {
    private final StockItemGroupRepository stockItemGroupRepository;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;

    public List<StockItemGroup> findBy(String code,
                                       Boolean isActive,
                                       @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                       @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StockItemGroup> criteriaQuery = criteriaBuilder.createQuery(StockItemGroup.class);
        Root<StockItemGroup> root = criteriaQuery.from(StockItemGroup.class);

        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.isNotBlank(code)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(StockItemGroup.Fields.code)),
                    "%" + "%s".formatted(code).toLowerCase() + "%"));
        }

        if (isActive != null) {
            predicates.add(criteriaBuilder.equal(root.get(StockItemGroup.Fields.isActive), isActive));
        }

        criteriaQuery.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public StockItemGroup save(@NotNull(message = "Stock item group can't be null") StockItemGroupDTO stockItemGroupDTO) {
        fieldValidator.validate(stockItemGroupDTO, StockItemGroupDTO.Fields.name, true);
        Optional<StockItemGroup> stockItemGroup = stockItemGroupRepository.findByName(stockItemGroupDTO.getName());

        if (stockItemGroup.isPresent()) {
            throw new ValidationException("Stock item group with name '%s' already exists");
        }

        return stockItemGroupRepository.save(
                StockItemGroup.builder()
                        .code(CodeGenerator.StockCodeGenerator.generate())
                        .name(stockItemGroupDTO.getName())
                        .isActive(stockItemGroupDTO.getIsActive() != null && stockItemGroupDTO.getIsActive())
                        .build());
    }

    public StockItemGroup update(@NotNull(message = "Group ID can't be null")
                                 @Min(value = 1, message = "Group ID can't be less than 1")
                                 Long groupId,
                                 String groupName,
                                 Boolean isActive) {
        StockItemGroup stockItemGroup = stockItemGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new NotFoundException("Stock group with ID '%s' was not found".formatted(groupId)));

        if (!StringUtils.isBlank(groupName)) {
            stockItemGroup.setName(groupName);
        }

        if (isActive != null) {
            stockItemGroup.setIsActive(isActive);
        }

        return stockItemGroupRepository.save(stockItemGroup);
    }
}
