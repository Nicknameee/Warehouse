package io.store.ua.repository;

import io.store.ua.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);

    boolean existsByTransactionIdOrReference(String transactionId, String reference);
}
