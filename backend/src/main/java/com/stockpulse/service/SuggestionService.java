package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.dto.SuggestionActionRequest;
import com.stockpulse.dto.SuggestionDto;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;

    public SuggestionService(PricingSuggestionRepository pricingRepo, ReorderSuggestionRepository reorderRepo) {
        this.pricingRepo = pricingRepo;
        this.reorderRepo = reorderRepo;
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
        // Phase 3 placeholder: DO NOT implement AI or rules yet
        // Returning null or a mock DTO is acceptable for the API skeleton.
        return new SuggestionDto(); 
    }

    @Transactional
    public SuggestionDto suggestReorder(String productId) {
        // Phase 3 placeholder: DO NOT implement AI or rules yet
        return new SuggestionDto();
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
        
        // Side effects (updating product price) belong to Phase 7
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
        
        // Side effects (updating product stock) belong to Phase 7
        return SuggestionDto.fromReorder(suggestion);
    }
}
