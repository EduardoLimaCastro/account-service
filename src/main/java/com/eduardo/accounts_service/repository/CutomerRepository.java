package com.eduardo.accounts_service.repository;

import com.eduardo.accounts_service.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CutomerRepository extends JpaRepository<CustomerEntity, Long> {
}
