package com.stockpulse.service.strategy;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;

public interface CommerceStrategy {
    SuggestionPair generateSuggestions(Product product, TriggerReason triggerReason, int categoryAvgVelocity);
}
