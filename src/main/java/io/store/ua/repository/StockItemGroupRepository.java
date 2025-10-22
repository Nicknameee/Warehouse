package io.store.ua.repository;

import io.store.ua.entity.StockItemGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockItemGroupRepository extends JpaRepository<StockItemGroup, Long> {
    Optional<StockItemGroup> findByName(String name);
    Optional<StockItemGroup> findByCode(String code);
}
