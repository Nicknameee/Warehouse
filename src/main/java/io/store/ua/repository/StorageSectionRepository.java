package io.store.ua.repository;

import io.store.ua.entity.StorageSection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorageSectionRepository extends JpaRepository<StorageSection, Long> {
    Page<StorageSection> findAllByWarehouseId(Long warehouseId, Pageable pageable);

    boolean existsByWarehouseIdAndCode(Long warehouseId, String code);

    Optional<StorageSection> findByWarehouseIdAndCode(Long warehouseId, String code);
}
