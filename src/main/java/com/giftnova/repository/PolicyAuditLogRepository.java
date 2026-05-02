package com.giftnova.repository;

import com.giftnova.model.PolicyAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PolicyAuditLogRepository extends JpaRepository<PolicyAuditLog, Long> {
    List<PolicyAuditLog> findByCompanyIdOrderByChangedAtDesc(Long companyId);
    void deleteByCompanyId(Long companyId);
}
