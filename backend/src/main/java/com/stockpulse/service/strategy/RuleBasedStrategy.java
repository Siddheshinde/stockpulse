package com.stockpulse.service.strategy;

import com.stockpulse.domain.PricingDirection;
import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("ruleBasedStrategy")
public class RuleBasedStrategy implements CommerceStrategy {

    @Override
    public SuggestionPair generateSuggestions(Product product, TriggerReason triggerReason, int categoryAvgVelocity) {
        PricingSuggestion pricing = generatePricing(product, triggerReason, categoryAvgVelocity);
        ReorderSuggestion reorder = generateReorder(product, triggerReason);
        return new SuggestionPair(pricing, reorder);
    }

    private PricingSuggestion generatePricing(Product p, TriggerReason triggerReason, int categoryAvgVelocity) {
        boolean lowStock = p.getStockLevel() < p.getReorderThreshold();
        boolean demandSpike = p.getDemandVelocity() > (categoryAvgVelocity * 2);

        BigDecimal currentPrice = p.getCurrentPrice();
        BigDecimal newPrice = currentPrice;
        PricingDirection direction = PricingDirection.HOLD;
        String reasoning = "No significant changes detected. Hold price.";

        if (lowStock) {
            // Documenting choice: Priority given to low-stock (protecting inventory) over demand spike if both occur.
            newPrice = currentPrice.multiply(new BigDecimal("1.10"));
            direction = PricingDirection.INCREASE;
            reasoning = "Low stock detected. Increase price by 10%.";
        } else if (demandSpike) {
            newPrice = currentPrice.multiply(new BigDecimal("1.05"));
            direction = PricingDirection.INCREASE;
            reasoning = "Demand spike detected. Increase price by 5%.";
        }

        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setProduct(p);
        suggestion.setCurrentPrice(currentPrice);
        suggestion.setRecommendedPrice(newPrice.setScale(2, RoundingMode.HALF_UP));
        suggestion.setDirection(direction);
        suggestion.setConfidence(1.0); // Rule-based is deterministic
        suggestion.setReasoning(reasoning);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(triggerReason);

        return suggestion;
    }

    private ReorderSuggestion generateReorder(Product p, TriggerReason triggerReason) {
        int reorderQty = Math.max((p.getReorderThreshold() * 3) - p.getStockLevel(), 1);

        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setProduct(p);
        suggestion.setCurrentStock(p.getStockLevel());
        suggestion.setRecommendedQuantity(reorderQty);
        suggestion.setSuggestedLeadTimeDays(7); // Default
        suggestion.setConfidence(1.0);
        suggestion.setReasoning("Rule-based stock replenishment formula applied.");
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setTriggerReason(triggerReason);

        return suggestion;
    }
}
