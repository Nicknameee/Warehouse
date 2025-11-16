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
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class TagService {
    private final TagRepository tagRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public List<Tag> findBy(String name,
                            Boolean isActive,
                            @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                            @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tag> criteriaQuery = criteriaBuilder.createQuery(Tag.class);
        Root<Tag> root = criteriaQuery.from(Tag.class);

        List<Predicate> predicates = new java.util.ArrayList<>();

        if (StringUtils.isNotBlank(name)) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Tag.Fields.name)),
                    "%" + name.toLowerCase() + "%"));
        }

        if (isActive != null) {
            predicates.add(criteriaBuilder.equal(root.get(Tag.Fields.isActive), isActive));
        }

        criteriaQuery
                .select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(criteriaBuilder.asc(root.get(Tag.Fields.id)));

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

    public Tag update(@NotNull(message = "Tag ID can't be null")
                      @Min(value = 1, message = "Tag ID can't be less than 1") Long tagId,
                      String tagName,
                      Boolean isActive) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NotFoundException("Tag with ID '%s' was not found".formatted(tagId)));

        if (!StringUtils.isBlank(tagName)) {
            tag.setName(tagName);
        }

        if (isActive != null) {
            tag.setIsActive(isActive);
        }

        return tagRepository.save(tag);
    }

    public void clearUnusedTags() {
        jdbcTemplate.execute(SqlResourceReader.getSQL("removeOrphanTags"));
    }
}
