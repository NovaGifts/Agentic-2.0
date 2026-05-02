package com.giftnova.repository;

import com.giftnova.model.GiftRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GiftRecommendationRepository extends JpaRepository<GiftRecommendation, Long> {
    Optional<GiftRecommendation> findByEventId(Long eventId);
    List<GiftRecommendation> findByCompanyId(Long companyId);
    void deleteByCompanyId(Long companyId);
}
