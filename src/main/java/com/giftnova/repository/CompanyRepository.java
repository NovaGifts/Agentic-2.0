package com.giftnova.repository;

import com.giftnova.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByName(String name);
    java.util.Optional<Company> findByName(String name);
    java.util.Optional<Company> findTopByOrderByCreatedAtDesc();
}
