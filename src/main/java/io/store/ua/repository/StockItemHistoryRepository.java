package io.store.ua.repository;

import io.store.ua.entity.immutable.StockItemHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemHistoryRepository extends JpaRepository<StockItemHistory, Long> {
}
