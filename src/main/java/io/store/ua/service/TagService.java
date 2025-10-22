package io.store.ua.service;

import io.store.ua.entity.Tag;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.repository.TagRepository;
import io.store.ua.utility.SqlResourceReader;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Validated
public class TagService {
    private final TagRepository tagRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<Tag> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                             @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return tagRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public List<Tag> findAllByIDs(@NotEmpty(message = "Tag ID list can't be empty")
                                  Set<
                                          @NotNull(message = "Tag ID can't be null")
                                          @Min(value = 1, message = "Tag ID can't be less than 1")
                                                  Long> tagIDs) {
        return tagRepository.findAllById(tagIDs);
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'OPERATOR')")
    public Tag save(@NotBlank(message = "Tag name can not be blank") String tagName) {
        Optional<Tag> tag = tagRepository.findByName(tagName);

        return tag.orElseGet(
                () -> tagRepository.save(Tag.builder().name(tagName).isActive(true).build()));
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'OPERATOR')")
    public Tag toggleState(@NotBlank(message = "Tag name can not be blank") String tagName) {
        Tag tag = tagRepository.findByName(tagName)
                .orElseThrow(() -> new NotFoundException("Tag with name: %s was not found".formatted(tagName)));

        tag.setIsActive(!tag.getIsActive());

        return tagRepository.save(tag);
    }

    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER')")
    public void emptyTags() {
        jdbcTemplate.execute(SqlResourceReader.getSQL("removeOrphanTags"));
    }
}
