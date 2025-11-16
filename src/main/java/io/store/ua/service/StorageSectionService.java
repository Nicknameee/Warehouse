package io.store.ua.service;

import io.store.ua.entity.StorageSection;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.exceptions.UniqueCheckException;
import io.store.ua.repository.StorageSectionRepository;
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

@Service
@RequiredArgsConstructor
@Validated
public class StorageSectionService {
    private final StorageSectionRepository storageSectionRepository;
    private final EntityManager entityManager;

    public List<StorageSection> findBy(@Min(value = 1, message = "Warehouse ID can't be less than 1")
                                       Long warehouseId,
                                       Boolean isActive,
                                       @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                       @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StorageSection> criteriaQuery = criteriaBuilder.createQuery(StorageSection.class);
        Root<StorageSection> root = criteriaQuery.from(StorageSection.class);

        List<Predicate> predicates = new ArrayList<>();

        if (warehouseId != null) {
            predicates.add(criteriaBuilder.equal(root.get(StorageSection.Fields.warehouseId), warehouseId));
        }

        if (isActive != null) {
            predicates.add(criteriaBuilder.equal(root.get(StorageSection.Fields.isActive), isActive));
        }

        criteriaQuery
                .select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(criteriaBuilder.asc(root.get(StorageSection.Fields.id)));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();

    }

    public StorageSection save(@NotNull(message = "Warehouse ID can't be null")
                               @Min(value = 1, message = "Warehouse ID can't be less than 1")
                               Long warehouseId,
                               @NotBlank(message = "Section code can't be blank") String code) {
        if (storageSectionRepository.existsByWarehouseIdAndCode(warehouseId, code)) {
            throw new UniqueCheckException("StorageSection with code '%s' already exists in warehouse with ID '%s'"
                    .formatted(code, warehouseId));
        }

        return storageSectionRepository.save(StorageSection.builder()
                .warehouseId(warehouseId)
                .code(code)
                .isActive(true)
                .build());
    }

    public StorageSection update(@NotNull(message = "Section ID can't be null")
                                 @Min(value = 1, message = "Section ID can't be less than 1")
                                 Long sectionId,
                                 Boolean isActive,
                                 @NotBlank(message = "Section code can't be blank") String newCode) {
        StorageSection existing = storageSectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("StorageSection id '%s' not found".formatted(sectionId)));

        if (storageSectionRepository.existsByWarehouseIdAndCode(existing.getWarehouseId(), newCode)) {
            throw new UniqueCheckException("StorageSection with code '%s' already exists in warehouse with ID '%s'"
                    .formatted(newCode, existing.getWarehouseId()));
        }

        existing.setCode(newCode);

        if (isActive != null) {
            existing.setIsActive(isActive);
        }

        return storageSectionRepository.save(existing);
    }
}
