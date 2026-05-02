package com.giftnova.service;

import com.giftnova.model.Company;
import com.giftnova.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository          companyRepository;
    private final EmployeeRepository         employeeRepo;
    private final UpcomingEventRepository    eventRepo;
    private final GiftRecommendationRepository recRepo;
    private final GiftPolicyRepository       policyRepo;
    private final PolicyAuditLogRepository   auditRepo;

    public CompanyService(CompanyRepository companyRepository,
                          EmployeeRepository employeeRepo,
                          UpcomingEventRepository eventRepo,
                          GiftRecommendationRepository recRepo,
                          GiftPolicyRepository policyRepo,
                          PolicyAuditLogRepository auditRepo) {
        this.companyRepository = companyRepository;
        this.employeeRepo      = employeeRepo;
        this.eventRepo         = eventRepo;
        this.recRepo           = recRepo;
        this.policyRepo        = policyRepo;
        this.auditRepo         = auditRepo;
    }

    public Company save(Company company) {
        return companyRepository.save(company);
    }

    public boolean exists(String name) {
        return companyRepository.existsByName(name);
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
    }

    public java.util.Optional<Company> findByName(String name) {
        return companyRepository.findByName(name);
    }

    public Company findLatest() {
        return companyRepository.findTopByOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("No companies found"));
    }

    @Transactional
    public void deleteCompany(Long companyId) {
        auditRepo.deleteByCompanyId(companyId);
        recRepo.deleteByCompanyId(companyId);
        eventRepo.deleteByCompanyId(companyId);
        policyRepo.deleteByCompanyId(companyId);
        employeeRepo.deleteByCompanyId(companyId);
        companyRepository.deleteById(companyId);
    }
}
