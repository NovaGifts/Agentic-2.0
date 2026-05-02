package com.giftnova.service;

import com.giftnova.model.Company;
import com.giftnova.repository.CompanyRepository;
import org.springframework.stereotype.Service;

/**
 * Service layer for Company operations.
 * Acts as the middleman between controllers and the CompanyRepository,
 * keeping database logic out of the controllers.
 */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    // Saves a new or updated company to the database
    public Company save(Company company) {
        return companyRepository.save(company);
    }

    // Checks if a company with the given name already exists (used to prevent duplicates)
    public boolean exists(String name) {
        return companyRepository.existsByName(name);
    }

    // Fetches a company by ID; throws if not found
    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
    }

    // Returns the most recently created company — used by /admin to auto-redirect in demo mode
    public Company findLatest() {
        return companyRepository.findTopByOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("No companies found"));
    }
}
