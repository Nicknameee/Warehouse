package io.store.ua.client;

import io.store.ua.AbstractIT;
import io.store.ua.entity.Tag;
import io.store.ua.entity.User;
import io.store.ua.enums.UserRole;
import io.store.ua.enums.UserStatus;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TagControllerIT extends AbstractIT {
    private static final String MANAGER = "manager";
    private HttpHeaders ownerHeaders;
    private HttpHeaders managerHeaders;

    @BeforeAll
    void setupAuthentication() {
        ownerHeaders = generateAuthenticationHeaders();
        managerHeaders = generateAuthenticationHeaders(MANAGER, MANAGER);
    }

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .username(MANAGER)
                .password(passwordEncoder.encode(MANAGER))
                .email("%s@example.com".formatted(MANAGER))
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .timezone("UTC")
                .build());
    }

    private Tag generateTag(boolean active) {
        return tagRepository.save(Tag.builder()
                .name(GENERATOR.nextAlphabetic(10))
                .isActive(active)
                .build());
    }

    @Nested
    @DisplayName("GET /api/v1/tags/findAll")
    class FindAllTagsTests {
        @Test
        @DisplayName("findAll_success_returnsPaginatedList")
        void findAll_success_returnsPaginatedList() {
            Tag firstTag = generateTag(true);
            Tag otherTag = generateTag(true);
            Tag anotherTag = generateTag(false);

            String firstPageUrl = UriComponentsBuilder.fromPath("/api/v1/tags/findAll")
                    .queryParam("pageSize", 1)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            String otherPageUrl = UriComponentsBuilder.fromPath("/api/v1/tags/findAll")
                    .queryParam("pageSize", 2)
                    .queryParam("page", 2)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Tag>> firstPage = restClient.exchange(firstPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });
            ResponseEntity<List<Tag>> otherPage = restClient.exchange(otherPageUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(firstPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(otherPage.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(firstPage.getBody())
                    .isNotNull()
                    .hasSize(1);
            assertThat(otherPage.getBody())
                    .isNotNull()
                    .hasSize(1);

            Set<String> returnedNames = firstPage.getBody()
                    .stream()
                    .map(Tag::getName)
                    .collect(Collectors.toSet());
            returnedNames.addAll(otherPage.getBody().stream().map(Tag::getName)
                    .toList());

            assertThat(returnedNames)
                    .containsAnyOf(firstTag.getName(), otherTag.getName(), anotherTag.getName());

            String managerUrl = UriComponentsBuilder.fromPath("/api/v1/tags/findAll")
                    .queryParam("pageSize", 5)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Tag>> pageManager = restClient.exchange(managerUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(managerHeaders),
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(pageManager.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(pageManager.getBody())
                    .isNotNull()
                    .isNotEmpty();
        }

        @Test
        @DisplayName("findAll_fails_invalidPagination_returns4xx")
        void findAll_fails_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/tags/findAll")
                    .queryParam("pageSize", 0)
                    .queryParam("page", -1)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> resp = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    String.class);

            assertThat(resp.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tags/findBy")
    class FindByTagsTests {
        @Test
        @DisplayName("findBy_success_filtersByNamesAndActive")
        void findBy_success_filtersByNamesAndActive() {
            Tag fooActive = generateTag(true);
            Tag barActive = generateTag(true);
            Tag bazActiveNoise = generateTag(true);
            Tag fooInactiveNoise = generateTag(false);

            String url = UriComponentsBuilder.fromPath("/api/v1/tags/findBy")
                    .queryParam("names", fooActive.getName())
                    .queryParam("names", barActive.getName())
                    .queryParam("isActive", true)
                    .queryParam("pageSize", 50)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Tag>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode()).
                    isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .isNotEmpty();

            List<Tag> result = response.getBody();
            Set<String> names = result
                    .stream()
                    .map(Tag::getName)
                    .collect(Collectors.toSet());
            assertThat(names)
                    .containsExactlyInAnyOrder(fooActive.getName(), barActive.getName());
            assertThat(result)
                    .allSatisfy(tag -> assertThat(tag.getIsActive())
                            .isTrue());
            assertThat(names)
                    .doesNotContain(bazActiveNoise.getName(), fooInactiveNoise.getName());
        }

        @Test
        @DisplayName("findBy_success_noFiltersReturnsAllPaged")
        void findBy_success_noFiltersReturnsAllPaged() {
            Tag firstTag = generateTag(true);
            Tag otherTag = generateTag(false);

            String url = UriComponentsBuilder.fromPath("/api/v1/tags/findBy")
                    .queryParam("pageSize", 10)
                    .queryParam("page", 1)
                    .build(true)
                    .toUriString();

            ResponseEntity<List<Tag>> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    new ParameterizedTypeReference<>() {
                    });

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull()
                    .isNotEmpty();

            Set<Long> returnedIds = response.getBody()
                    .stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet());

            assertThat(returnedIds)
                    .contains(firstTag.getId(), otherTag.getId());
        }

        @Test
        @DisplayName("findBy_fails_invalidPagination_returns4xx")
        void findBy_fails_invalidPagination_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/tags/findBy")
                    .queryParam("pageSize", 0)
                    .queryParam("page", 0)
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.GET,
                    new HttpEntity<>(ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tags")
    class SaveTagTests {
        @Test
        @DisplayName("save_success_createsTag")
        void save_success_createsTag() {
            String nameToCreate = GENERATOR.nextAlphabetic(30);

            String url = UriComponentsBuilder.fromPath("/api/v1/tags")
                    .queryParam("name", nameToCreate)
                    .build(true)
                    .toUriString();

            ResponseEntity<Tag> response = restClient.exchange(url,
                    HttpMethod.POST,
                    new HttpEntity<>(ownerHeaders),
                    Tag.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            Tag persisted = tagRepository.findByName(nameToCreate)
                    .orElseThrow();
            assertThat(response.getBody().getId())
                    .isEqualTo(persisted.getId());
            assertThat(response.getBody().getName())
                    .isEqualTo(persisted.getName());
            assertThat(response.getBody().getIsActive())
                    .isEqualTo(persisted.getIsActive());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tags")
    class UpdateTagTests {
        @Test
        @DisplayName("update_success_updatesFields")
        void update_success_updatesFields() {
            Tag existing = generateTag(true);
            String updatedName = GENERATOR.nextAlphabetic(30);

            String url = UriComponentsBuilder.fromPath("/api/v1/tags")
                    .queryParam("id", existing.getId())
                    .queryParam("name", updatedName)
                    .queryParam("isActive", false)
                    .build(true)
                    .toUriString();

            ResponseEntity<Tag> response = restClient.exchange(url,
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerHeaders),
                    Tag.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .isNotNull();

            Tag persisted = tagRepository.findById(existing.getId())
                    .orElseThrow();

            assertThat(response.getBody().getId())
                    .isEqualTo(persisted.getId());
            assertThat(response.getBody().getName())
                    .isEqualTo(persisted.getName());
            assertThat(response.getBody().getIsActive())
                    .isEqualTo(persisted.getIsActive());
            assertThat(persisted.getName())
                    .isEqualTo(updatedName);
            assertThat(persisted.getIsActive())
                    .isFalse();
        }

        @Test
        @DisplayName("update_fails_missingId_returns4xx")
        void update_fails_missingId_returns4xx() {
            String url = UriComponentsBuilder.fromPath("/api/v1/tags")
                    .queryParam("name", GENERATOR.nextAlphabetic(999))
                    .build(true)
                    .toUriString();

            ResponseEntity<String> response = restClient.exchange(url,
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerHeaders),
                    String.class);

            assertThat(response.getStatusCode().is4xxClientError())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tags/clearAll")
    class ClearAllTagsTests {
        @Test
        @DisplayName("emptyTags_success_ownerAllowed")
        void emptyTags_success_ownerAllowed() {
            generateTag(true);
            generateTag(false);
            generateTag(false);

            long initialCount = tagRepository.count();

            assertThat(initialCount).isEqualTo(3);

            String url = UriComponentsBuilder.fromPath("/api/v1/tags/clearAll")
                    .build(true)
                    .toUriString();

            ResponseEntity<Void> response = restClient.exchange(url,
                    HttpMethod.PUT,
                    new HttpEntity<>(ownerHeaders),
                    Void.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            long count = tagRepository.count();

            assertThat(count)
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("emptyTags_success_managerAllowed")
        void emptyTags_success_managerAllowed() {
            String url = UriComponentsBuilder.fromPath("/api/v1/tags/clearAll")
                    .build(true)
                    .toUriString();

            ResponseEntity<Void> response = restClient.exchange(url,
                    HttpMethod.PUT,
                    new HttpEntity<>(managerHeaders),
                    Void.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }
    }
}
