package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.event.ProductStateChangedEvent;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.strategy.CommerceStrategy;
import com.stockpulse.service.strategy.SuggestionPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AgenticRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AgenticRecommendationService.class);

    private final ProductRepository productRepo;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final Map<String, CommerceStrategy> strategies;
    private final Environment env;

    public AgenticRecommendationService(ProductRepository productRepo,
                                        PricingSuggestionRepository pricingRepo,
                                        ReorderSuggestionRepository reorderRepo,
                                        Map<String, CommerceStrategy> strategies,
                                        Environment env) {
        this.productRepo = productRepo;
        this.pricingRepo = pricingRepo;
        this.reorderRepo = reorderRepo;
        this.strategies = strategies;
        this.env = env;
    }

    @Async
    @EventListener
    public void handleProductStateChanged(ProductStateChangedEvent event) {
        log.info("Processing recommendation event for Product: {}. Trigger reason: {}", 
                 event.getProductId(), event.getTriggerReason());

        String lockKey = (event.getProductId() + "-" + event.getTriggerReason().name()).intern();
        synchronized (lockKey) {

        try {
            Product p = productRepo.findById(event.getProductId()).orElse(null);
            if (p == null) {
                log.warn("Product {} not found during event processing. Skipping.", event.getProductId());
                return;
            }

            boolean pricingExists = pricingRepo.existsByProduct_IdAndTriggerReasonAndStatus(
                    event.getProductId(), event.getTriggerReason(), SuggestionStatus.PENDING);
            boolean reorderExists = reorderRepo.existsByProduct_IdAndTriggerReasonAndStatus(
                    event.getProductId(), event.getTriggerReason(), SuggestionStatus.PENDING);

            if (pricingExists && reorderExists) {
                log.info("Skipping duplicate pending suggestions for Product: {} and Trigger: {}", 
                         event.getProductId(), event.getTriggerReason());
                return;
            }

            int avgVelocity = productRepo.getAverageVelocityByCategory(p.getCategory()).intValue();
            String strategyName = env.getProperty("commerce.strategy", "ruleBasedStrategy");
            CommerceStrategy strategy = strategies.get(strategyName);

            SuggestionPair pair = strategy.generateSuggestions(p, event.getTriggerReason(), avgVelocity);

            if (!pricingExists) {
                pricingRepo.save(pair.getPricing());
                log.info("Created pricing suggestion for Product: {}", event.getProductId());
            } else {
                log.info("Skipping duplicate pending pricing suggestion for Product: {}", event.getProductId());
            }

            if (!reorderExists) {
                reorderRepo.save(pair.getReorder());
                log.info("Created reorder suggestion for Product: {}", event.getProductId());
            } else {
                log.info("Skipping duplicate pending reorder suggestion for Product: {}", event.getProductId());
            }
            
        } catch (Exception e) {
            log.error("Failed to process ProductStateChangedEvent for product: {}", event.getProductId(), e);
        }
        }
    }
}
