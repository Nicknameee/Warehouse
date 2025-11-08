package io.store.ua.repository;

import io.store.ua.entity.StorageSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageSectionRepository extends JpaRepository<StorageSection, Long> {
    boolean existsByIdAndWarehouseId(Long id, Long warehouseId);

    boolean existsByWarehouseIdAndCode(Long warehouseId, String code);
}
