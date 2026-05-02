package com.giftnova.repository;

import com.giftnova.model.UpcomingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UpcomingEventRepository extends JpaRepository<UpcomingEvent, Long> {
    List<UpcomingEvent> findByCompanyIdOrderByEventDate(Long companyId);
    void deleteByCompanyId(Long companyId);
    Optional<UpcomingEvent> findByRecipientToken(String recipientToken);
    Optional<UpcomingEvent> findByManagerToken(String managerToken);
}
