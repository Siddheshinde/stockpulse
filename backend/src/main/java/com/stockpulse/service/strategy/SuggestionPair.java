package com.stockpulse.service.strategy;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.ReorderSuggestion;

public class SuggestionPair {
    private PricingSuggestion pricing;
    private ReorderSuggestion reorder;

    public SuggestionPair(PricingSuggestion pricing, ReorderSuggestion reorder) {
        this.pricing = pricing;
        this.reorder = reorder;
    }

    public PricingSuggestion getPricing() { return pricing; }
    public ReorderSuggestion getReorder() { return reorder; }
}
