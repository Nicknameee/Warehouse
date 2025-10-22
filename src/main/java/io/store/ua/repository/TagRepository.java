package io.store.ua.repository;

import io.store.ua.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query(
            value = "SELECT t.id FROM Tag t ORDER BY t.id ASC",
            countQuery = "SELECT COUNT(t) FROM Tag t"
    )
    Page<Long> findAllIDs(Pageable pageable);

    @EntityGraph(attributePaths = Tag.Fields.links)
    List<Tag> findDistinctByIdIn(Set<Long> ids);

    Optional<Tag> findByName(String name);
}
