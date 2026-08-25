package com.stockpulse.event;

import com.stockpulse.domain.TriggerReason;

public class ProductStateChangedEvent {
    
    private final String productId;
    private final TriggerReason triggerReason;
    
    public ProductStateChangedEvent(Object source, String productId, TriggerReason triggerReason) {
        this.productId = productId;
        this.triggerReason = triggerReason;
    }

    public String getProductId() {
        return productId;
    }

    public TriggerReason getTriggerReason() {
        return triggerReason;
    }
}
