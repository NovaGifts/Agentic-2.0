package com.giftnova.repository;

import com.giftnova.model.UpcomingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UpcomingEventRepository extends JpaRepository<UpcomingEvent, Long> {
    List<UpcomingEvent> findByCompanyIdOrderByEventDate(Long companyId);
    // Used to wipe and regenerate events on each CSV upload
    void deleteByCompanyId(Long companyId);
}
