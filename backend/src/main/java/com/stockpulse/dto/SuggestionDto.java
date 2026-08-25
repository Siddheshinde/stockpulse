package com.stockpulse.dto;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;

public class SuggestionDto {
    private Long id;
    private String productId;
    private String type; // "PRICING" or "REORDER"
    private TriggerReason triggerReason;
    
    // Values formatted as strings to accommodate both money and quantity
    private String currentValue;
    private String recommendedValue;
    
    // For pricing: INCREASE/DECREASE/HOLD. For reorder: could just be "REORDER" or null
    private String direction; 
    
    private Double confidence;
    private String reasoning;
    private SuggestionStatus status;

    public static SuggestionDto fromPricing(PricingSuggestion p) {
        SuggestionDto dto = new SuggestionDto();
        dto.id = p.getId();
        dto.productId = p.getProduct().getId();
        dto.type = "PRICING";
        dto.triggerReason = p.getTriggerReason();
        dto.currentValue = p.getCurrentPrice() != null ? p.getCurrentPrice().toString() : "";
        dto.recommendedValue = p.getRecommendedPrice() != null ? p.getRecommendedPrice().toString() : "";
        dto.direction = p.getDirection() != null ? p.getDirection().name() : "";
        dto.confidence = p.getConfidence();
        dto.reasoning = p.getReasoning();
        dto.status = p.getStatus();
        return dto;
    }

    public static SuggestionDto fromReorder(ReorderSuggestion r) {
        SuggestionDto dto = new SuggestionDto();
        dto.id = r.getId();
        dto.productId = r.getProduct().getId();
        dto.type = "REORDER";
        dto.triggerReason = r.getTriggerReason();
        dto.currentValue = r.getCurrentStock() != null ? r.getCurrentStock().toString() : "";
        dto.recommendedValue = r.getRecommendedQuantity() != null ? r.getRecommendedQuantity().toString() : "";
        dto.direction = "REORDER";
        dto.confidence = r.getConfidence();
        dto.reasoning = r.getReasoning();
        dto.status = r.getStatus();
        return dto;
    }

    // Getters
    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public String getType() { return type; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public String getCurrentValue() { return currentValue; }
    public String getRecommendedValue() { return recommendedValue; }
    public String getDirection() { return direction; }
    public Double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
}
