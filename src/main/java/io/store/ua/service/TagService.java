package io.store.ua.service;

import io.store.ua.entity.Tag;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.repository.TagRepository;
import io.store.ua.utility.SqlResourceReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class TagService {
    private final TagRepository tagRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public List<Tag> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                             @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return tagRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public List<Tag> findBy(List<@NotBlank(message = "Tag name cannot be blank") String> names,
                            Boolean isActive,
                            @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                            @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tag> criteriaQuery = criteriaBuilder.createQuery(Tag.class);
        Root<Tag> root = criteriaQuery.from(Tag.class);

        List<Predicate> predicates = new java.util.ArrayList<>();

        if (names != null && !names.isEmpty()) {
            predicates.add(root.get(Tag.Fields.name).in(names));
        }

        if (isActive != null) {
            predicates.add(criteriaBuilder.equal(root.get(Tag.Fields.isActive), isActive));
        }

        criteriaQuery.select(root);

        if (!predicates.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(Predicate[]::new)));
        }

        criteriaQuery.orderBy(criteriaBuilder.desc(root.get(Tag.Fields.id)));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Tag save(@NotBlank(message = "Tag name can not be blank") String tagName) {
        return tagRepository.findByName(tagName).orElseGet(() -> tagRepository.save(Tag.builder()
                .name(tagName)
                .isActive(true)
                .build()));
    }

    public Tag changeState(@NotBlank(message = "Tag name can't be blank") String tagName,
                           @NotNull(message = "New state can't be null") Boolean isActive) {
        Tag tag = tagRepository.findByName(tagName)
                .orElseThrow(() -> new NotFoundException("Tag with name '%s' was not found".formatted(tagName)));

        tag.setIsActive(isActive);

        return tagRepository.save(tag);
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public void emptyTags() {
        jdbcTemplate.execute(SqlResourceReader.getSQL("removeOrphanTags"));
    }
}
