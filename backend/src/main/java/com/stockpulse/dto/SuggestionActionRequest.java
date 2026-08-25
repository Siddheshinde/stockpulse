package com.stockpulse.dto;

import com.stockpulse.domain.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public class SuggestionActionRequest {
    @NotNull
    private SuggestionStatus status; // Usually ACCEPTED or REJECTED

    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }
}
