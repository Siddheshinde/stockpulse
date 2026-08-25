package com.stockpulse.service.strategy;

import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.service.ai.LLMGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("aiStrategy")
public class AiStrategy implements CommerceStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AiStrategy.class);
    
    private final LLMGateway llmGateway;
    private final CommerceStrategy fallbackStrategy;

    public AiStrategy(LLMGateway llmGateway, @Qualifier("ruleBasedStrategy") CommerceStrategy fallbackStrategy) {
        this.llmGateway = llmGateway;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public SuggestionPair generateSuggestions(Product product, TriggerReason triggerReason, int categoryAvgVelocity) {
        try {
            if (!llmGateway.isConfigured()) {
                logger.warn("Gemini API key is missing. Falling back to RuleBasedStrategy.");
                return fallbackStrategy.generateSuggestions(product, triggerReason, categoryAvgVelocity);
            }
            
            logger.info("Calling Gemini AI for product: {}, trigger: {}", product.getId(), triggerReason);
            return llmGateway.generateSuggestions(product, triggerReason, categoryAvgVelocity);
            
        } catch (Exception e) {
            logger.error("AI Strategy failed. Falling back to RuleBasedStrategy. Reason: {}", e.getMessage());
            return fallbackStrategy.generateSuggestions(product, triggerReason, categoryAvgVelocity);
        }
    }
}
