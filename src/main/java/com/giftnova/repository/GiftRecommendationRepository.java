package com.giftnova.repository;

import com.giftnova.model.GiftRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GiftRecommendationRepository extends JpaRepository<GiftRecommendation, Long> {
    // Look up an existing recommendation by event — used to avoid regenerating unnecessarily
    Optional<GiftRecommendation> findByEventId(Long eventId);
}
