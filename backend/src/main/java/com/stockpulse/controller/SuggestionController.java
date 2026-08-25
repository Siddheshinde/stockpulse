package com.stockpulse.controller;

import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.dto.SuggestionActionRequest;
import com.stockpulse.dto.SuggestionDto;
import com.stockpulse.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<SuggestionDto>> getSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        return ResponseEntity.ok(suggestionService.getPendingSuggestions(status));
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public ResponseEntity<SuggestionDto> updatePricingSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionActionRequest req) {
        return ResponseEntity.ok(suggestionService.acceptOrRejectPricing(id, req));
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public ResponseEntity<SuggestionDto> updateReorderSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionActionRequest req) {
        return ResponseEntity.ok(suggestionService.acceptOrRejectReorder(id, req));
    }
}
