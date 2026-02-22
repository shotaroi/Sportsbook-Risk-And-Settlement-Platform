package com.shotaroi.sportsbook.betting.repository;

import com.shotaroi.sportsbook.betting.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
