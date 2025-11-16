package io.store.ua.service;

import io.store.ua.entity.Beneficiary;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.BeneficiaryDTO;
import io.store.ua.repository.BeneficiaryRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;

    public List<Beneficiary> findBy(String IBANPrefix,
                                    String SWIFTPrefix,
                                    String cardPrefix,
                                    String namePart,
                                    Boolean isActive,
                                    @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                    @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Beneficiary> criteriaQuery = criteriaBuilder.createQuery(Beneficiary.class);
        Root<Beneficiary> root = criteriaQuery.from(Beneficiary.class);

        List<Predicate> predicates = new ArrayList<>();

        if (!StringUtils.isBlank(IBANPrefix)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.iban)), IBANPrefix.toLowerCase() + "%"));
        }

        if (!StringUtils.isBlank(SWIFTPrefix)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.swift)), SWIFTPrefix.toLowerCase() + "%"));
        }

        if (!StringUtils.isBlank(cardPrefix)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.card)), cardPrefix.toLowerCase() + "%"));
        }

        if (!StringUtils.isBlank(namePart)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.name)), "%" + namePart.toLowerCase() + "%"));
        }

        if (isActive != null) {
            predicates.add(criteriaBuilder.equal(root.get(Beneficiary.Fields.isActive), isActive));
        }

        criteriaQuery.select(root).where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Beneficiary save(@NotNull(message = "Beneficiary can't be null") BeneficiaryDTO beneficiaryDTO) {
        fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.name);

        if (StringUtils.isAllBlank(beneficiaryDTO.getIban(), beneficiaryDTO.getSwift(), beneficiaryDTO.getCard())) {
            throw new ValidationException("Beneficiary can't be created without any bank account information");
        }

        if ((StringUtils.isNotBlank(beneficiaryDTO.getSwift()) && StringUtils.isBlank(beneficiaryDTO.getIban()))
                || (StringUtils.isBlank(beneficiaryDTO.getSwift()) && StringUtils.isNotBlank(beneficiaryDTO.getIban()))) {
            throw new ValidationException("Beneficiary can't have only one of IBAN or SWIFT");
        }

        fieldValidator.validate(beneficiaryDTO, true,
                BeneficiaryDTO.Fields.iban,
                BeneficiaryDTO.Fields.swift);
        fieldValidator.validate(beneficiaryDTO, false, BeneficiaryDTO.Fields.card);

        if (StringUtils.isNotBlank(beneficiaryDTO.getIban())
                && beneficiaryRepository.existsByIban(beneficiaryDTO.getIban())) {
            throw new BusinessException("Beneficiary with IBAN '%s' already exists"
                    .formatted(beneficiaryDTO.getIban()));
        }

        if (StringUtils.isNotBlank(beneficiaryDTO.getCard())
                && beneficiaryRepository.existsByCard(beneficiaryDTO.getCard())) {
            throw new BusinessException("Beneficiary with card '%s' already exists"
                    .formatted(beneficiaryDTO.getCard()));
        }

        return beneficiaryRepository.save(Beneficiary.builder()
                .name(beneficiaryDTO.getName())
                .iban(beneficiaryDTO.getIban())
                .swift(beneficiaryDTO.getSwift())
                .card(beneficiaryDTO.getCard())
                .isActive(beneficiaryDTO.getIsActive() != null && beneficiaryDTO.getIsActive())
                .build());
    }

    public Beneficiary update(@NotNull(message = "Beneficiary can't be null") BeneficiaryDTO beneficiaryDTO) {
        fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.id);
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryDTO.getId())
                .orElseThrow(() -> new NotFoundException("Beneficiary with ID '%s' was not found".formatted(beneficiaryDTO.getId())));

        if (StringUtils.isNotBlank(beneficiaryDTO.getName())) {
            fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.name);

            if (!beneficiaryDTO.getName().equals(beneficiary.getName())) {
                beneficiary.setName(beneficiaryDTO.getName());
            }
        }

        if (StringUtils.isNotBlank(beneficiaryDTO.getIban())) {
            fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.iban);

            if (!beneficiaryDTO.getIban().equals(beneficiary.getIban()) && beneficiaryRepository.existsByIban(beneficiaryDTO.getIban())) {
                throw new BusinessException("Beneficiary with IBAN '%s' already exists".formatted(beneficiaryDTO.getIban()));
            }

            beneficiary.setIban(beneficiaryDTO.getIban());
        }

        if (StringUtils.isNotBlank(beneficiaryDTO.getSwift())) {
            fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.swift);

            if (!beneficiaryDTO.getSwift().equals(beneficiary.getSwift())) {
                beneficiary.setSwift(beneficiaryDTO.getSwift());
            }
        }

        if (StringUtils.isNotBlank(beneficiaryDTO.getCard())) {
            fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.card);

            if (!beneficiaryDTO.getCard().equals(beneficiary.getCard()) && beneficiaryRepository.existsByCard(beneficiaryDTO.getCard())) {
                throw new BusinessException("Beneficiary with card '%s' already exists".formatted(beneficiaryDTO.getCard()));
            }

            beneficiary.setCard(beneficiaryDTO.getCard());
        }

        if (beneficiaryDTO.getIsActive() != null) {
            beneficiary.setIsActive(beneficiaryDTO.getIsActive());
        }

        if (StringUtils.isAllBlank(beneficiary.getIban(), beneficiary.getSwift(), beneficiary.getCard())) {
            throw new ValidationException("Beneficiary can't be created without any bank account information");
        }

        if ((StringUtils.isNotBlank(beneficiary.getSwift()) && StringUtils.isBlank(beneficiary.getIban()))
                || (StringUtils.isBlank(beneficiary.getSwift()) && StringUtils.isNotBlank(beneficiary.getIban()))) {
            throw new ValidationException("Beneficiary can't have only one of IBAN or SWIFT");
        }

        return beneficiaryRepository.save(beneficiary);
    }

    public List<Beneficiary> changeState(@NotEmpty(message = "Beneficiary IDs can't be empty") List<
                                                 @NotNull(message = "ID can't be null")
                                                 @Min(value = 1, message = "ID can't be less than 1")
                                                         Long> beneficiaryIDs,
                                         @NotNull(message = "Active flag can't be null") Boolean isActive) {
        var beneficiaries = beneficiaryRepository.findAllById(beneficiaryIDs);

        beneficiaries.forEach(beneficiary -> beneficiary.setIsActive(isActive));

        return beneficiaryRepository.saveAll(beneficiaries);
    }
}
