package com.stockpulse.repository;

import com.stockpulse.domain.PricingSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {
}
