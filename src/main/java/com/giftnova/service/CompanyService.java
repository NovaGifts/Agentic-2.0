package com.giftnova.service;

import com.giftnova.model.Company;
import com.giftnova.repository.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company save(Company company) {
        return companyRepository.save(company);
    }

    public boolean exists(String name) {
        return companyRepository.existsByName(name);
    }
}
