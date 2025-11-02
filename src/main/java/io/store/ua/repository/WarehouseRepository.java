package io.store.ua.repository;

import io.store.ua.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);

    Optional<Warehouse> findByName(String name);
}
