package com.giftnova.repository;

import com.giftnova.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByCompanyId(Long companyId);
    Optional<Employee> findByCompanyIdAndEmail(Long companyId, String email);
    void deleteByCompanyId(Long companyId);
}
