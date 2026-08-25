package com.stockpulse.service.ai;

import java.math.BigDecimal;

public class LLMResponseDto {
    private Pricing pricing;
    private Reorder reorder;

    public Pricing getPricing() { return pricing; }
    public void setPricing(Pricing pricing) { this.pricing = pricing; }
    public Reorder getReorder() { return reorder; }
    public void setReorder(Reorder reorder) { this.reorder = reorder; }

    public static class Pricing {
        private BigDecimal recommendedPrice;
        private String direction;
        private Double confidence;
        private String reasoning;

        public BigDecimal getRecommendedPrice() { return recommendedPrice; }
        public void setRecommendedPrice(BigDecimal recommendedPrice) { this.recommendedPrice = recommendedPrice; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    }

    public static class Reorder {
        private Integer recommendedQuantity;
        private Double confidence;
        private String reasoning;

        public Integer getRecommendedQuantity() { return recommendedQuantity; }
        public void setRecommendedQuantity(Integer recommendedQuantity) { this.recommendedQuantity = recommendedQuantity; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    }
}
