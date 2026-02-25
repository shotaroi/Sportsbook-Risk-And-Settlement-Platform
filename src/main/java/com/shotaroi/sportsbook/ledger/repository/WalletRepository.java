package com.shotaroi.sportsbook.ledger.repository;

import com.shotaroi.sportsbook.ledger.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByCustomerIdAndCurrency(Long customerId, String currency);
}
