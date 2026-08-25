package com.stockpulse.service.strategy;

import com.stockpulse.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleBasedStrategyTest {

    private RuleBasedStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RuleBasedStrategy();
    }

    @Test
    void testHoldPricingAndStandardReorder() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setStockLevel(50);
        p.setReorderThreshold(20);
        p.setDemandVelocity(5);

        SuggestionPair pair = strategy.generateSuggestions(p, TriggerReason.MANUAL, 10);

        assertEquals(PricingDirection.HOLD, pair.getPricing().getDirection());
        assertEquals(new BigDecimal("100.00"), pair.getPricing().getRecommendedPrice());

        // Max((20 * 3) - 50, 1) = Max(60 - 50, 1) = 10
        assertEquals(10, pair.getReorder().getRecommendedQuantity());
    }

    @Test
    void testLowStockPricing() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setStockLevel(10);
        p.setReorderThreshold(20);
        p.setDemandVelocity(5);

        SuggestionPair pair = strategy.generateSuggestions(p, TriggerReason.MANUAL, 10);

        assertEquals(PricingDirection.INCREASE, pair.getPricing().getDirection());
        assertEquals(new BigDecimal("110.00"), pair.getPricing().getRecommendedPrice());
        
        // Max((20 * 3) - 10, 1) = Max(60 - 10, 1) = 50
        assertEquals(50, pair.getReorder().getRecommendedQuantity());
    }

    @Test
    void testDemandSpikePricing() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setStockLevel(50);
        p.setReorderThreshold(20);
        p.setDemandVelocity(25); // > 2x category avg (10)

        SuggestionPair pair = strategy.generateSuggestions(p, TriggerReason.MANUAL, 10);

        assertEquals(PricingDirection.INCREASE, pair.getPricing().getDirection());
        assertEquals(new BigDecimal("105.00"), pair.getPricing().getRecommendedPrice());
        
        // Max((20 * 3) - 50, 1) = Max(60 - 50, 1) = 10
        assertEquals(10, pair.getReorder().getRecommendedQuantity());
    }

    @Test
    void testCombinedPricingTakesPrecedenceOnLowStock() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setStockLevel(10); // Low stock
        p.setReorderThreshold(20);
        p.setDemandVelocity(25); // Demand spike

        SuggestionPair pair = strategy.generateSuggestions(p, TriggerReason.MANUAL, 10);

        // Precedence applies low stock (+10%)
        assertEquals(PricingDirection.INCREASE, pair.getPricing().getDirection());
        assertEquals(new BigDecimal("110.00"), pair.getPricing().getRecommendedPrice());
        
        // Max((20 * 3) - 10, 1) = 50
        assertEquals(50, pair.getReorder().getRecommendedQuantity());
    }

    @Test
    void testMinimumReorderQuantity() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setStockLevel(100);
        p.setReorderThreshold(20);
        p.setDemandVelocity(5); 

        SuggestionPair pair = strategy.generateSuggestions(p, TriggerReason.MANUAL, 10);

        // Max((20 * 3) - 100, 1) = Max(60 - 100, 1) = 1
        assertEquals(1, pair.getReorder().getRecommendedQuantity());
    }
}
