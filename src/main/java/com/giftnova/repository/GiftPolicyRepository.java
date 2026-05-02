package com.giftnova.repository;

import com.giftnova.model.GiftPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GiftPolicyRepository extends JpaRepository<GiftPolicy, Long> {
    List<GiftPolicy> findByCompanyIdOrderByEventType(Long companyId);
    long countByCompanyId(Long companyId);
    // Used by EventGeneratorService to get the budget for a specific event type
    java.util.Optional<com.giftnova.model.GiftPolicy> findByCompanyIdAndEventType(
            Long companyId, com.giftnova.model.EventType eventType);
    void deleteByCompanyId(Long companyId);
}
