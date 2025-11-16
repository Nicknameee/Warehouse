package io.store.ua.controllers;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Beneficiary;
import io.store.ua.models.dto.BeneficiaryDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BeneficiaryControllerIT extends AbstractIT {
    private HttpHeaders ownerAuthenticationHeaders;

    @BeforeAll
    void setupAuthentication() {
        ownerAuthenticationHeaders = generateAuthenticationHeaders();
    }

    private Beneficiary generateBeneficiary(String name, String IBAN, String SWIFT, String card, boolean isActive) {
        return beneficiaryRepository.save(Beneficiary.builder()
                .name(name)
                .iban(IBAN)
                .swift(SWIFT)
                .card(card)
                .isActive(isActive)
                .build());
    }

    private BeneficiaryDTO generateBeneficiaryDTO() {
        return BeneficiaryDTO.builder()
                .name(GENERATOR.nextAlphanumeric(10))
                .iban("UA%s".formatted(GENERATOR.nextNumeric(27)))
                .swift("SW%s".formatted(GENERATOR.nextAlphanumeric(6).toUpperCase()))
                .card(GENERATOR.nextNumeric(16))
                .isActive(true)
                .build();
    }

    private HttpHeaders generateHeaders(HttpHeaders original) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(original);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Nested
    @DisplayName("GET /api/v1/beneficiaries/findBy")
    class FindByTests {
        @Test
        @DisplayName("findBy_success_appliesAllFilters")
        void findBy_success_appliesAllFilters() {
            var beneficiary = generateBeneficiary(GENERATOR.nextAlphabetic(10),
                    "UA%s".formatted(GENERATOR.nextNumeric(27)),
                    GENERATOR.nextAlphanumeric(6).toUpperCase(),
                    "5375%s".formatted(GENERATOR.nextNumeric(12)),
                    true);
            generateBeneficiary(GENERATOR.nextAlphabetic(10),
                    "UA%s".formatted(GENERATOR.nextNumeric(27)),
                    GENERATOR.nextAlphanumeric(6).toUpperCase(),
                    "4149%s".formatted(GENERATOR.nextNumeric(12)),
                    false);

            String url = UriComponentsBuilder.fromPath("/api/v1/beneficiaries/findBy")
                    .queryParam("IBANPrefix", beneficiary.getIban().substring(0, 3))
                    .queryParam("SWIFTPrefix", beneficiary.getSwift().substring(0, 3))
                    .queryParam("cardPrefix", beneficiary.getCard().substring(0, 4))
                    .queryParam("namePart", beneficiary.getName().substring(0, 5))
                    .queryParam("isActive", beneficiary.getIsActive())
                    .queryParam("pageSize", 5)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Beneficiary>> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .extracting(Beneficiary::getId)
                    .containsExactly(beneficiary.getId());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/beneficiaries")
    class SaveTests {
        @Test
        @DisplayName("save_success_persistsEntity")
        void save_success_persistsEntity() {
            var beneficiaryDTO = generateBeneficiaryDTO();

            ResponseEntity<Beneficiary> responseEntity = restClient.exchange(
                    "/api/v1/beneficiaries",
                    HttpMethod.POST,
                    new HttpEntity<>(beneficiaryDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Beneficiary.class);

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(beneficiaryRepository.findById(responseEntity.getBody().getId()).isPresent())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/beneficiaries")
    class UpdateTests {
        @Test
        @DisplayName("update_success_updatesSelectedFields")
        void update_success_updatesSelectedFields() {
            var beneficiary = generateBeneficiary(GENERATOR.nextAlphabetic(10),
                    "UA%s".formatted(GENERATOR.nextNumeric(27)),
                    GENERATOR.nextAlphanumeric(6).toUpperCase(),
                    "4149%s".formatted(GENERATOR.nextNumeric(12)),
                    true);
            var beneficiaryDTO = BeneficiaryDTO.builder()
                    .id(beneficiary.getId())
                    .name(GENERATOR.nextAlphabetic(30))
                    .swift(GENERATOR.nextAlphanumeric(6).toUpperCase())
                    .build();

            ResponseEntity<Beneficiary> responseEntity = restClient.exchange(
                    "/api/v1/beneficiaries",
                    HttpMethod.PUT,
                    new HttpEntity<>(beneficiaryDTO, generateHeaders(ownerAuthenticationHeaders)),
                    Beneficiary.class);

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull();
            assertThat(responseEntity.getBody().getName())
                    .isEqualTo(beneficiaryDTO.getName());
            assertThat(responseEntity.getBody().getSwift())
                    .isEqualTo(beneficiaryDTO.getSwift());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/beneficiaries/changeState")
    class ChangeStateTests {
        @Test
        @DisplayName("changeState_success_updatesFlags")
        void changeState_success_updatesFlags() {
            var beneficiary = generateBeneficiary(GENERATOR.nextAlphabetic(10),
                    "UA%s".formatted(GENERATOR.nextNumeric(27)),
                    GENERATOR.nextAlphanumeric(6).toUpperCase(),
                    "40000000%s".formatted(GENERATOR.nextNumeric(8)),
                    false);
            var otherBeneficiary = generateBeneficiary(GENERATOR.nextAlphabetic(10),
                    "UA%s".formatted(GENERATOR.nextNumeric(27)),
                    GENERATOR.nextAlphanumeric(6).toUpperCase(),
                    "4%s".formatted(GENERATOR.nextNumeric(15)),
                    false);
            var anotherBeneficiary = generateBeneficiary(GENERATOR.nextAlphabetic(10),
                    "UA%s".formatted(GENERATOR.nextNumeric(27)),
                    GENERATOR.nextAlphanumeric(6).toUpperCase(),
                    "4%s".formatted(GENERATOR.nextNumeric(1)),
                    false);

            String url = UriComponentsBuilder.fromPath("/api/v1/beneficiaries/changeState")
                    .queryParam("beneficiaryID", beneficiary.getId())
                    .queryParam("beneficiaryID", otherBeneficiary.getId())
                    .queryParam("beneficiaryID", anotherBeneficiary.getId())
                    .queryParam("isActive", true)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Beneficiary>> responseEntity = restClient.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerAuthenticationHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(responseEntity.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .hasSize(3);
            assertThat(responseEntity.getBody())
                    .allSatisfy(beneficiaryAccount ->
                            assertThat(beneficiaryAccount.getIsActive()).isTrue());
        }
    }
}
