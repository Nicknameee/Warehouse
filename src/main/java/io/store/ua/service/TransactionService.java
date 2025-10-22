package io.store.ua.service;

import io.store.ua.entity.Transaction;
import io.store.ua.repository.TransactionRepository;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public List<Transaction> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                     @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return transactionRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }
}
