package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.BeneficiaryDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeneficiaryServiceIT extends AbstractIT {
    @Autowired
    private BeneficiaryService beneficiaryService;

    private List<Beneficiary> generateBeneficiaries(int count) {
        return Stream.generate(this::generateBeneficiary)
                .limit(count)
                .toList();
    }

    @Nested
    @DisplayName("save(beneficiaryDTO: BeneficiaryDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: creates a new Beneficiary with provided bank details")
        @Transactional
        void save_success() {
            BeneficiaryDTO beneficiaryDTO = BeneficiaryDTO.builder()
                    .name(GENERATOR.nextAlphabetic(10))
                    .iban("UA" + GENERATOR.nextNumeric(27))
                    .swift(GENERATOR.nextAlphabetic(8).toUpperCase())
                    .build();

            Beneficiary beneficiary = beneficiaryService.save(beneficiaryDTO);

            assertThat(beneficiary.getId()).isNotNull();
            assertThat(beneficiary.getName()).isEqualTo(beneficiaryDTO.getName());
            assertThat(beneficiary.getIban()).isEqualTo(beneficiaryDTO.getIban());
            assertThat(beneficiary.getSwift()).isEqualTo(beneficiaryDTO.getSwift());
            assertThat(beneficiary.getCard()).isNull();
            assertThat(beneficiaryRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("save_fail_whenNoBankInfo: save fails when no financial information is present")
        void save_fail_whenNoBankInfo() {
            BeneficiaryDTO beneficiaryDTO = BeneficiaryDTO.builder()
                    .name(GENERATOR.nextAlphabetic(10))
                    .build();

            assertThatThrownBy(() -> beneficiaryService.save(beneficiaryDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("save_fail_whenDuplicateIBANOrCard: save fails when card or IBAN information is already taken")
        @Transactional
        void save_fail_whenDuplicateIBANOrCard() {
            String iban = "UA" + GENERATOR.nextNumeric(27);
            String card = GENERATOR.nextNumeric(16);

            beneficiaryRepository.save(Beneficiary.builder()
                    .name(GENERATOR.nextAlphabetic(10))
                    .iban(iban)
                    .card(card)
                    .isActive(true)
                    .build());

            BeneficiaryDTO beneficiaryDTOWithWrongIban = BeneficiaryDTO.builder()
                    .name(GENERATOR.nextAlphabetic(10))
                    .iban(iban)
                    .card(GENERATOR.nextNumeric(16))
                    .build();
            BeneficiaryDTO beneficiaryDTOWithWrongCardNumber = BeneficiaryDTO.builder()
                    .name(GENERATOR.nextAlphabetic(10))
                    .iban("UA" + GENERATOR.nextNumeric(27))
                    .card(card)
                    .build();

            assertThatThrownBy(() -> beneficiaryService.save(beneficiaryDTOWithWrongIban))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> beneficiaryService.save(beneficiaryDTOWithWrongCardNumber))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest
        @NullSource
        void save_fail_whenNullDTO(BeneficiaryDTO beneficiaryDTO) {
            assertThatThrownBy(() -> beneficiaryService.save(beneficiaryDTO))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("findAll(pageSize: int, page: int)")
    class FindAllTests {
        @ParameterizedTest
        @CsvSource({"1,1", "3,2", "5,3"})
        @Transactional
        void findAll_success(int pageSize, int page) {
            beneficiaryRepository.saveAll(generateBeneficiaries(pageSize * page));

            var beneficiaries = beneficiaryService.findAll(pageSize, page);

            assertThat(beneficiaries).hasSize(pageSize);
            assertThat(beneficiaries).allSatisfy(b -> assertThat(b.getId()).isNotNull());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -5})
        void findAll_fail_whenInvalidPageSize(int size) {
            assertThatThrownBy(() -> beneficiaryService.findAll(size, 1))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        void findAll_fail_whenInvalidPage(int page) {
            assertThatThrownBy(() -> beneficiaryService.findAll(10, page))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("findBy(...)")
    class FindByTests {
        @Test
        @DisplayName("findBy_success: filters correctly by prefixes")
        @Transactional
        void findBy_success() {
            String ibanPrefix = "UA12%s".formatted(GENERATOR.nextNumeric(23));
            String swiftPrefix = GENERATOR.nextAlphabetic(4).toUpperCase();
            String cardPrefix = GENERATOR.nextNumeric(4);

            Beneficiary firstBeneficiary = Beneficiary.builder()
                    .name(GENERATOR.nextAlphabetic(8))
                    .iban("%s%s".formatted(ibanPrefix, GENERATOR.nextNumeric(4)))
                    .swift("%s%s".formatted(swiftPrefix, GENERATOR.nextAlphabetic(4).toUpperCase()))
                    .card("%s%s".formatted(cardPrefix, GENERATOR.nextNumeric(12)))
                    .isActive(true)
                    .build();
            Beneficiary otherBeneficiary = Beneficiary.builder()
                    .name(GENERATOR.nextAlphabetic(8))
                    .iban("%s%s".formatted(ibanPrefix, GENERATOR.nextNumeric(4)))
                    .swift("%s%s".formatted(swiftPrefix, GENERATOR.nextAlphabetic(4).toUpperCase()))
                    .card("%s%s".formatted(cardPrefix, GENERATOR.nextNumeric(12)))
                    .isActive(false)
                    .build();

            List<Beneficiary> beneficiaries = new ArrayList<>(generateBeneficiaries(5));

            beneficiaries.add(firstBeneficiary);
            beneficiaries.add(otherBeneficiary);

            beneficiaryRepository.saveAll(beneficiaries);

            beneficiaries = beneficiaryService.findBy(ibanPrefix,
                    swiftPrefix,
                    cardPrefix,
                    "",
                    true,
                    5,
                    1);

            assertThat(beneficiaries)
                    .isNotEmpty();
            assertThat(beneficiaries)
                    .allMatch(beneficiary -> beneficiary.getIban().startsWith(ibanPrefix)
                            || beneficiary.getSwift().startsWith(swiftPrefix)
                            || beneficiary.getCard().startsWith(cardPrefix));
            assertThat(beneficiaries)
                    .allMatch(Beneficiary::getIsActive);
        }
    }

    @Nested
    @DisplayName("update(beneficiaryDTO: BeneficiaryDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success: updates all fields when provided")
        @Transactional
        void update_success() {
            Beneficiary beneficiary = beneficiaryRepository.save(Beneficiary.builder()
                    .name(GENERATOR.nextAlphabetic(8))
                    .iban("UA%s".formatted(GENERATOR.nextNumeric(27)))
                    .swift(GENERATOR.nextAlphabetic(8).toUpperCase())
                    .card(GENERATOR.nextNumeric(16))
                    .isActive(true)
                    .build());

            BeneficiaryDTO beneficiaryDTO = BeneficiaryDTO.builder()
                    .id(beneficiary.getId())
                    .name(GENERATOR.nextAlphabetic(8))
                    .iban("UA%s".formatted(GENERATOR.nextNumeric(27)))
                    .swift(GENERATOR.nextAlphabetic(8).toUpperCase())
                    .card(GENERATOR.nextNumeric(16))
                    .isActive(false)
                    .build();

            long initialCount = beneficiaryRepository.count();

            beneficiary = beneficiaryService.update(beneficiaryDTO);

            assertThat(beneficiary.getName())
                    .isEqualTo(beneficiaryDTO.getName());
            assertThat(beneficiary.getIban())
                    .isEqualTo(beneficiaryDTO.getIban());
            assertThat(beneficiary.getSwift())
                    .isEqualTo(beneficiaryDTO.getSwift());
            assertThat(beneficiary.getCard())
                    .isEqualTo(beneficiaryDTO.getCard());
            assertThat(beneficiary.getIsActive())
                    .isEqualTo(beneficiaryDTO.getIsActive());
            assertThat(beneficiaryRepository.count())
                    .isEqualTo(initialCount);
        }

        @Test
        @DisplayName("update_fail_whenMissingId: update fails when no beneficiary ID is present")
        void update_fail_whenMissingId() {
            BeneficiaryDTO beneficiaryDTO = BeneficiaryDTO.builder()
                    .name(GENERATOR.nextAlphabetic(909))
                    .build();

            assertThatThrownBy(() -> beneficiaryService.update(beneficiaryDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fail_whenNotFound: update fails when no beneficiary found by ID")
        void update_fail_whenNotFound() {
            BeneficiaryDTO beneficiaryDTO = BeneficiaryDTO.builder()
                    .id(RandomUtils.secure().randomLong(1, 100_000))
                    .name(GENERATOR.nextAlphabetic(99))
                    .build();

            assertThatThrownBy(() -> beneficiaryService.update(beneficiaryDTO))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("update_fail_whenDuplicateIBANOrCard: update fails when suggested new financial info already registered in the system")
        @Transactional
        void update_fail_whenDuplicateIBANOrCard() {
            String initialIBAN = "UA%s".formatted(GENERATOR.nextNumeric(27));
            String initialCard = GENERATOR.nextNumeric(16);
            String newIBAN = "UA%s".formatted(GENERATOR.nextNumeric(27));
            String newCard = GENERATOR.nextNumeric(16);

            Beneficiary beneficiary = beneficiaryRepository.save(Beneficiary.builder()
                    .name(GENERATOR.nextAlphabetic(8))
                    .iban(initialIBAN)
                    .card(initialCard)
                    .isActive(true)
                    .build());

            beneficiaryRepository.save(Beneficiary.builder()
                    .name(GENERATOR.nextAlphabetic(8))
                    .iban(newIBAN)
                    .card(newCard)
                    .isActive(true)
                    .build());

            assertThatThrownBy(() -> beneficiaryService.update(BeneficiaryDTO.builder()
                    .id(beneficiary.getId())
                    .iban(newIBAN)
                    .build()))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> beneficiaryService.update(BeneficiaryDTO.builder()
                    .id(beneficiary.getId())
                    .card(newCard)
                    .build()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("changeState(IDs: List<Long>, isActive: Boolean)")
    class ChangeStateTests {
        @Test
        @DisplayName("changeState_success: changes state to given flag for given beneficiaries by their ID")
        @Transactional
        void changeState_success() {
            List<Long> beneficiaryIDs = beneficiaryRepository.saveAll(generateBeneficiaries(3))
                    .stream()
                    .map(Beneficiary::getId)
                    .toList();

            var deactivated = beneficiaryService.changeState(beneficiaryIDs, false);
            assertThat(deactivated).allMatch(beneficiary -> Boolean.FALSE.equals(beneficiary.getIsActive()));

            var reactivated = beneficiaryService.changeState(beneficiaryIDs, true);
            assertThat(reactivated).allMatch(beneficiary -> Boolean.TRUE.equals(beneficiary.getIsActive()));
        }

        @Test
        @DisplayName("changeState_fail_whenEmptyIDs: change state fails when no IDs present")
        void changeState_fail_whenEmptyIds() {
            assertThatThrownBy(() -> beneficiaryService.changeState(List.of(), true))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("changeState_fail_whenInvalidIdsOrFlag: change state fails when invalid IDs or flag are given")
        void changeState_fail_whenInvalidIdsOrFlag() {
            assertThatThrownBy(() -> beneficiaryService.changeState(List.of(0L, -1L), true))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> beneficiaryService.changeState(List.of(1L), null))
                    .isInstanceOf(ConstraintViolationException.class);
        }
    }
}
