package io.store.ua.repository;

import io.store.ua.entity.Tag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {
    @EntityGraph(attributePaths = Tag.Fields.links)
    List<Tag> findDistinctByIdIn(Set<Long> ids);

    Optional<Tag> findByName(String name);
}
