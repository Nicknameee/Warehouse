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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;
    private final FieldValidator fieldValidator;
    private final EntityManager entityManager;

    public List<Beneficiary> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                     @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return beneficiaryRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public List<Beneficiary> findBy(String IBANPrefix,
                                    String SWIFTPrefix,
                                    String cardPrefix,
                                    String name,
                                    @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                    @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Beneficiary> criteriaQuery = criteriaBuilder.createQuery(Beneficiary.class);
        Root<Beneficiary> root = criteriaQuery.from(Beneficiary.class);

        List<Predicate> predicates = new ArrayList<>();

        if (!StringUtils.isBlank(IBANPrefix)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.IBAN)), IBANPrefix.toLowerCase() + "%"));
        }

        if (!StringUtils.isBlank(SWIFTPrefix)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.SWIFT)), SWIFTPrefix.toLowerCase() + "%"));
        }

        if (!StringUtils.isBlank(cardPrefix)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.card)), cardPrefix.toLowerCase() + "%"));
        }

        if (!StringUtils.isBlank(name)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Beneficiary.Fields.name)), "%" + name.toLowerCase() + "%"));
        }

        criteriaQuery.select(root).where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Beneficiary save(@NotNull(message = "Beneficiary can't be null") BeneficiaryDTO beneficiaryDTO) {
        fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.name);

        if (StringUtils.isAllBlank(beneficiaryDTO.getIBAN(), beneficiaryDTO.getSWIFT(), beneficiaryDTO.getCard())) {
            throw new ValidationException("Beneficiary can't be created without any bank account information");
        }

        fieldValidator.validate(beneficiaryDTO, false,
                BeneficiaryDTO.Fields.IBAN,
                BeneficiaryDTO.Fields.SWIFT,
                BeneficiaryDTO.Fields.card);

        if (beneficiaryRepository.existsByIBANOrCard(beneficiaryDTO.getIBAN(), beneficiaryDTO.getCard())) {
            throw new BusinessException("Beneficiary with IBAN '%s' or card '%s' already exists".formatted(beneficiaryDTO.getIBAN(), beneficiaryDTO.getCard()));
        }

        return beneficiaryRepository.save(Beneficiary.builder()
                .name(beneficiaryDTO.getName())
                .IBAN(beneficiaryDTO.getIBAN())
                .SWIFT(beneficiaryDTO.getSWIFT())
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
            beneficiary.setName(beneficiaryDTO.getName());
        }

        if (StringUtils.isNotBlank(beneficiaryDTO.getIBAN())) {
            fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.IBAN);

            if (beneficiaryRepository.existsByIBAN(beneficiaryDTO.getIBAN())) {
                throw new BusinessException("Beneficiary with IBAN '%s' already exists".formatted(beneficiaryDTO.getIBAN()));
            }

            beneficiary.setIBAN(beneficiaryDTO.getIBAN());
        }

        if (StringUtils.isNotBlank(beneficiaryDTO.getSWIFT())) {
            fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.SWIFT);
            beneficiary.setSWIFT(beneficiaryDTO.getSWIFT());
        }

        if (StringUtils.isNotBlank(beneficiaryDTO.getCard())) {
            fieldValidator.validate(beneficiaryDTO, true, BeneficiaryDTO.Fields.card);

            if (beneficiaryRepository.existsByCard(beneficiaryDTO.getCard())) {
                throw new BusinessException("Beneficiary with card '%s' already exists".formatted(beneficiaryDTO.getCard()));
            }

            beneficiary.setCard(beneficiaryDTO.getCard());
        }

        return beneficiaryRepository.save(beneficiary);
    }

    public List<Beneficiary> changeState(@NotEmpty(message = "IDs can't be empty") List<
                                                 @NotNull(message = "ID can't be null")
                                                 @Min(value = 1, message = "ID can't be less than 1") Long> IDs,
                                         @NotNull(message = "Active flag can't be null") Boolean isActive) {
        var beneficiaries = beneficiaryRepository.findAllById(IDs);

        beneficiaries.forEach(beneficiary -> beneficiary.setIsActive(isActive));

        return beneficiaryRepository.saveAll(beneficiaries);
    }
}
