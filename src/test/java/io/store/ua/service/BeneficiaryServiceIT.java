package io.store.ua.service;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.exceptions.BusinessException;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.BeneficiaryDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeneficiaryServiceIT extends AbstractIT {
    @Autowired
    private BeneficiaryService beneficiaryService;

    private List<Beneficiary> generateBeneficiaries(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(ignore -> Beneficiary.builder()
                        .name(RandomStringUtils.secure().nextAlphabetic(10))
                        .IBAN("UA" + RandomStringUtils.secure().nextNumeric(27))
                        .SWIFT(RandomStringUtils.secure().nextAlphabetic(8).toUpperCase())
                        .card(RandomStringUtils.secure().nextNumeric(16))
                        .isActive(true)
                        .build())
                .toList();
    }

    @Nested
    @DisplayName("save(beneficiaryDTO: BeneficiaryDTO)")
    class SaveTests {
        @Test
        @DisplayName("save_success: creates a new Beneficiary with provided bank details")
        @Transactional
        void save_success() {
            var request = BeneficiaryDTO.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(10))
                    .IBAN("UA" + RandomStringUtils.secure().nextNumeric(27))
                    .build();

            var result = beneficiaryService.save(request);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo(request.getName());
            assertThat(result.getIBAN()).isEqualTo(request.getIBAN());
            assertThat(result.getSWIFT()).isNull();
            assertThat(result.getCard()).isNull();
            assertThat(beneficiaryRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("save_fail_whenNoBankInfo: save fails when no financial information is present")
        void save_fail_whenNoBankInfo() {
            var request = BeneficiaryDTO.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(10))
                    .build();

            assertThatThrownBy(() -> beneficiaryService.save(request))
                    .isInstanceOf(jakarta.validation.ValidationException.class);
        }

        @Test
        @DisplayName("save_fail_whenDuplicateIBANOrCard: save fails when card or IBAN information is already taken")
        @Transactional
        void save_fail_whenDuplicateIBANOrCard() {
            var iban = "UA" + RandomStringUtils.secure().nextNumeric(27);
            var card = RandomStringUtils.secure().nextNumeric(16);

            beneficiaryRepository.save(Beneficiary.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(10))
                    .IBAN(iban)
                    .card(card)
                    .isActive(true)
                    .build());

            var duplicateIban = BeneficiaryDTO.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(10))
                    .IBAN(iban)
                    .build();
            var duplicateCard = BeneficiaryDTO.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(10))
                    .card(card)
                    .build();

            assertThatThrownBy(() -> beneficiaryService.save(duplicateIban))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> beneficiaryService.save(duplicateCard))
                    .isInstanceOf(BusinessException.class);
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

            var result = beneficiaryService.findAll(pageSize, page);

            assertThat(result).hasSize(pageSize);
            assertThat(result).allSatisfy(b -> assertThat(b.getId()).isNotNull());
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
            var ibanPrefix = "UA12" + RandomStringUtils.secure().nextNumeric(23);
            var swiftPrefix = RandomStringUtils.secure().nextAlphabetic(4).toUpperCase();
            var cardPrefix = RandomStringUtils.secure().nextNumeric(4);

            var firstTargetBeneficiary = Beneficiary.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(8))
                    .IBAN(ibanPrefix + RandomStringUtils.secure().nextNumeric(4))
                    .SWIFT(swiftPrefix + RandomStringUtils.secure().nextAlphabetic(4).toUpperCase())
                    .card(cardPrefix + RandomStringUtils.secure().nextNumeric(12))
                    .isActive(true)
                    .build();

            var otherTargetBeneficiary = Beneficiary.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(8))
                    .IBAN(ibanPrefix + RandomStringUtils.secure().nextNumeric(4))
                    .SWIFT(swiftPrefix + RandomStringUtils.secure().nextAlphabetic(4).toUpperCase())
                    .card(cardPrefix + RandomStringUtils.secure().nextNumeric(12))
                    .isActive(true)
                    .build();

            var beneficiaryPool = new ArrayList<>(generateBeneficiaries(5));

            beneficiaryPool.add(firstTargetBeneficiary);
            beneficiaryPool.add(otherTargetBeneficiary);

            beneficiaryRepository.saveAll(beneficiaryPool);

            var result = beneficiaryService.findBy(ibanPrefix, swiftPrefix, cardPrefix, "", 5, 1);

            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(beneficiary -> beneficiary.getIBAN().startsWith(ibanPrefix)
                    || beneficiary.getSWIFT().startsWith(swiftPrefix)
                    || beneficiary.getCard().startsWith(cardPrefix));
        }
    }

    @Nested
    @DisplayName("update(beneficiaryDTO: BeneficiaryDTO)")
    class UpdateTests {
        @Test
        @DisplayName("update_success: updates all fields when provided")
        @Transactional
        void update_success() {
            var initialBeneficiaries = beneficiaryRepository.save(Beneficiary.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(8))
                    .IBAN("UA" + RandomStringUtils.secure().nextNumeric(27))
                    .SWIFT(RandomStringUtils.secure().nextAlphabetic(8).toUpperCase())
                    .card(RandomStringUtils.secure().nextNumeric(16))
                    .isActive(true)
                    .build());

            var beneficiaryDTO = BeneficiaryDTO.builder()
                    .id(initialBeneficiaries.getId())
                    .name(RandomStringUtils.secure().nextAlphabetic(8))
                    .IBAN("UA" + RandomStringUtils.secure().nextNumeric(27))
                    .SWIFT(RandomStringUtils.secure().nextAlphabetic(8).toUpperCase())
                    .card(RandomStringUtils.secure().nextNumeric(16))
                    .build();

            var result = beneficiaryService.update(beneficiaryDTO);

            assertThat(result.getName()).isEqualTo(beneficiaryDTO.getName());
            assertThat(result.getIBAN()).isEqualTo(beneficiaryDTO.getIBAN());
            assertThat(result.getSWIFT()).isEqualTo(beneficiaryDTO.getSWIFT());
            assertThat(result.getCard()).isEqualTo(beneficiaryDTO.getCard());
        }

        @Test
        @DisplayName("update_fail_whenMissingId: update fails when no beneficiary ID is present")
        void update_fail_whenMissingId() {
            var beneficiaryDTO = BeneficiaryDTO.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(909))
                    .build();

            assertThatThrownBy(() -> beneficiaryService.update(beneficiaryDTO))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("update_fail_whenNotFound: update fails when no beneficiary found by ID")
        void update_fail_whenNotFound() {
            var beneficiaryDTO = BeneficiaryDTO.builder()
                    .id(RandomUtils.secure().randomLong(1, 100_000))
                    .name(RandomStringUtils.secure().nextAlphabetic(99))
                    .build();

            assertThatThrownBy(() -> beneficiaryService.update(beneficiaryDTO))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("update_fail_whenDuplicateIBANOrCard: update fails when suggested new financial info already registered in the system")
        @Transactional
        void update_fail_whenDuplicateIBANOrCard() {
            var initialIBAN = "UA" + RandomStringUtils.secure().nextNumeric(27);
            var initialCard = RandomStringUtils.secure().nextNumeric(16);
            var newIBAN = "UA" + RandomStringUtils.secure().nextNumeric(27);
            var newCard = RandomStringUtils.secure().nextNumeric(16);

            var initialBeneficiary = beneficiaryRepository.save(Beneficiary.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(8))
                    .IBAN(initialIBAN)
                    .card(initialCard)
                    .isActive(true)
                    .build());

            beneficiaryRepository.save(Beneficiary.builder()
                    .name(RandomStringUtils.secure().nextAlphabetic(8))
                    .IBAN(newIBAN)
                    .card(newCard)
                    .isActive(true)
                    .build());

            assertThatThrownBy(() -> beneficiaryService.update(BeneficiaryDTO.builder().id(initialBeneficiary.getId()).IBAN(newIBAN).build()))
                    .isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> beneficiaryService.update(BeneficiaryDTO.builder().id(initialBeneficiary.getId()).card(newCard).build()))
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
            var initialBeneficiaryIDs = beneficiaryRepository.saveAll(generateBeneficiaries(3))
                    .stream()
                    .map(Beneficiary::getId)
                    .toList();

            var deactivated = beneficiaryService.changeState(initialBeneficiaryIDs, false);
            assertThat(deactivated).allMatch(beneficiary -> Boolean.FALSE.equals(beneficiary.getIsActive()));

            var reactivated = beneficiaryService.changeState(initialBeneficiaryIDs, true);
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
