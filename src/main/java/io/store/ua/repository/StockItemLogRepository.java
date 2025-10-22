package io.store.ua.repository;

import io.store.ua.entity.immutable.StockItemLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockItemLogRepository extends JpaRepository<StockItemLog, Long> {}
