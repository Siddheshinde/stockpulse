package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.dto.SuggestionActionRequest;
import com.stockpulse.dto.SuggestionDto;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.service.strategy.CommerceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SuggestionServiceTest {

    @Mock
    private PricingSuggestionRepository pricingRepo;

    @Mock
    private ReorderSuggestionRepository reorderRepo;

    @Mock
    private ProductRepository productRepo;

    @Mock
    private Environment env;

    @Mock
    private Map<String, CommerceStrategy> strategies;

    @InjectMocks
    private SuggestionService suggestionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void acceptPricingSuggestion_UpdatesPrice() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setCurrentPrice(new BigDecimal("10.00"));

        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setId(1L);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setProduct(p);
        suggestion.setRecommendedPrice(new BigDecimal("15.00"));

        when(pricingRepo.findById(1L)).thenReturn(Optional.of(suggestion));
        when(pricingRepo.save(any(PricingSuggestion.class))).thenReturn(suggestion);
        when(productRepo.save(any(Product.class))).thenReturn(p);

        SuggestionActionRequest req = new SuggestionActionRequest();
        req.setStatus(SuggestionStatus.ACCEPTED);

        SuggestionDto result = suggestionService.acceptOrRejectPricing(1L, req);

        assertEquals(SuggestionStatus.ACCEPTED, result.getStatus());
        assertEquals(new BigDecimal("15.00"), p.getCurrentPrice());
        verify(productRepo).save(p);
    }

    @Test
    void rejectPricingSuggestion_DoesNotUpdatePrice() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setCurrentPrice(new BigDecimal("10.00"));

        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setId(1L);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setProduct(p);
        suggestion.setRecommendedPrice(new BigDecimal("15.00"));

        when(pricingRepo.findById(1L)).thenReturn(Optional.of(suggestion));
        when(pricingRepo.save(any(PricingSuggestion.class))).thenReturn(suggestion);

        SuggestionActionRequest req = new SuggestionActionRequest();
        req.setStatus(SuggestionStatus.REJECTED);

        SuggestionDto result = suggestionService.acceptOrRejectPricing(1L, req);

        assertEquals(SuggestionStatus.REJECTED, result.getStatus());
        assertEquals(new BigDecimal("10.00"), p.getCurrentPrice());
        verify(productRepo, never()).save(p);
    }

    @Test
    void acceptReorderSuggestion_UpdatesStock() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setStockLevel(5);

        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setId(1L);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setProduct(p);
        suggestion.setRecommendedQuantity(20);

        when(reorderRepo.findById(1L)).thenReturn(Optional.of(suggestion));
        when(reorderRepo.save(any(ReorderSuggestion.class))).thenReturn(suggestion);
        when(productRepo.save(any(Product.class))).thenReturn(p);

        SuggestionActionRequest req = new SuggestionActionRequest();
        req.setStatus(SuggestionStatus.ACCEPTED);

        SuggestionDto result = suggestionService.acceptOrRejectReorder(1L, req);

        assertEquals(SuggestionStatus.ACCEPTED, result.getStatus());
        assertEquals(25, p.getStockLevel());
        verify(productRepo).save(p);
    }

    @Test
    void rejectReorderSuggestion_DoesNotUpdateStock() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setStockLevel(5);

        ReorderSuggestion suggestion = new ReorderSuggestion();
        suggestion.setId(1L);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setProduct(p);
        suggestion.setRecommendedQuantity(20);

        when(reorderRepo.findById(1L)).thenReturn(Optional.of(suggestion));
        when(reorderRepo.save(any(ReorderSuggestion.class))).thenReturn(suggestion);

        SuggestionActionRequest req = new SuggestionActionRequest();
        req.setStatus(SuggestionStatus.REJECTED);

        SuggestionDto result = suggestionService.acceptOrRejectReorder(1L, req);

        assertEquals(SuggestionStatus.REJECTED, result.getStatus());
        assertEquals(5, p.getStockLevel());
        verify(productRepo, never()).save(p);
    }

    @Test
    void acceptAlreadyProcessedSuggestion_ThrowsException() {
        PricingSuggestion suggestion = new PricingSuggestion();
        suggestion.setId(1L);
        suggestion.setStatus(SuggestionStatus.ACCEPTED);

        when(pricingRepo.findById(1L)).thenReturn(Optional.of(suggestion));

        SuggestionActionRequest req = new SuggestionActionRequest();
        req.setStatus(SuggestionStatus.ACCEPTED);

        assertThrows(IllegalStateException.class, () -> {
            suggestionService.acceptOrRejectPricing(1L, req);
        });
    }
}
