package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.dto.SuggestionActionRequest;
import com.stockpulse.dto.SuggestionDto;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.strategy.CommerceStrategy;
import com.stockpulse.service.strategy.SuggestionPair;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final ProductRepository productRepo;
    private final Map<String, CommerceStrategy> strategies;
    private final Environment env;

    public SuggestionService(PricingSuggestionRepository pricingRepo, 
                             ReorderSuggestionRepository reorderRepo,
                             ProductRepository productRepo,
                             Map<String, CommerceStrategy> strategies,
                             Environment env) {
        this.pricingRepo = pricingRepo;
        this.reorderRepo = reorderRepo;
        this.productRepo = productRepo;
        this.strategies = strategies;
        this.env = env;
    }

    @Transactional(readOnly = true)
    public List<SuggestionDto> getPendingSuggestions(SuggestionStatus status) {
        List<SuggestionDto> suggestions = new ArrayList<>();
        
        suggestions.addAll(pricingRepo.findAll().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .map(SuggestionDto::fromPricing)
                .collect(Collectors.toList()));
                
        suggestions.addAll(reorderRepo.findAll().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .map(SuggestionDto::fromReorder)
                .collect(Collectors.toList()));
                
        return suggestions;
    }

    @Transactional
    public SuggestionDto suggestPricing(String productId) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
                
        int avgVelocity = productRepo.getAverageVelocityByCategory(p.getCategory()).intValue();
        String strategyName = env.getProperty("commerce.strategy", "ruleBasedStrategy");
        CommerceStrategy strategy = strategies.get(strategyName);
        
        SuggestionPair pair = strategy.generateSuggestions(p, TriggerReason.MANUAL, avgVelocity);
        
        pricingRepo.save(pair.getPricing());
        reorderRepo.save(pair.getReorder());
        
        return SuggestionDto.fromPricing(pair.getPricing());
    }

    @Transactional
    public SuggestionDto suggestReorder(String productId) {
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
                
        int avgVelocity = productRepo.getAverageVelocityByCategory(p.getCategory()).intValue();
        String strategyName = env.getProperty("commerce.strategy", "ruleBasedStrategy");
        CommerceStrategy strategy = strategies.get(strategyName);
        
        SuggestionPair pair = strategy.generateSuggestions(p, TriggerReason.MANUAL, avgVelocity);
        
        pricingRepo.save(pair.getPricing());
        reorderRepo.save(pair.getReorder());
        
        return SuggestionDto.fromReorder(pair.getReorder());
    }

    @Transactional
    public SuggestionDto acceptOrRejectPricing(Long id, SuggestionActionRequest req) {
        PricingSuggestion suggestion = pricingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing suggestion not found"));
                
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already " + suggestion.getStatus());
        }
        
        if (req.getStatus() != SuggestionStatus.ACCEPTED && req.getStatus() != SuggestionStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid action. Must be ACCEPTED or REJECTED");
        }
        
        suggestion.setStatus(req.getStatus());
        suggestion = pricingRepo.save(suggestion);
        
        if (req.getStatus() == SuggestionStatus.ACCEPTED) {
            Product p = suggestion.getProduct();
            p.setCurrentPrice(suggestion.getRecommendedPrice());
            productRepo.save(p);
        }
        
        return SuggestionDto.fromPricing(suggestion);
    }

    @Transactional
    public SuggestionDto acceptOrRejectReorder(Long id, SuggestionActionRequest req) {
        ReorderSuggestion suggestion = reorderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder suggestion not found"));
                
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already " + suggestion.getStatus());
        }
        
        if (req.getStatus() != SuggestionStatus.ACCEPTED && req.getStatus() != SuggestionStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid action. Must be ACCEPTED or REJECTED");
        }
        
        suggestion.setStatus(req.getStatus());
        suggestion = reorderRepo.save(suggestion);
        
        if (req.getStatus() == SuggestionStatus.ACCEPTED) {
            Product p = suggestion.getProduct();
            p.setStockLevel(p.getStockLevel() + suggestion.getRecommendedQuantity());
            productRepo.save(p);
        }
        
        return SuggestionDto.fromReorder(suggestion);
    }
}
