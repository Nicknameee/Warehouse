package io.store.ua.repository;

import io.store.ua.entity.immutable.StockItemHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemHistoryRepository extends JpaRepository<StockItemHistory, Long> {
    long countByStockItemId(Long stockItemId);

    Page<StockItemHistory> findByStockItemId(Long stockItemId, Pageable pageable);
}
