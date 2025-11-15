package io.store.ua.repository;

import io.store.ua.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    boolean existsByIbanOrCard(String IBAN, String cardNumber);

    boolean existsByIban(String IBAN);

    boolean existsByCard(String cardNumber);
}
