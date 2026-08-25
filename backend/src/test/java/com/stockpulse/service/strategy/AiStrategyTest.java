package com.stockpulse.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.Category;
import com.stockpulse.domain.PricingDirection;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.service.ai.LLMGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiStrategyTest {

    private LLMGateway llmGateway;
    private CommerceStrategy fallbackStrategy;
    private AiStrategy aiStrategy;
    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        fallbackStrategy = mock(CommerceStrategy.class);
        llmGateway = new LLMGateway("fake-api-key", new ObjectMapper());
        mockHttpClient = mock(HttpClient.class);
        mockHttpResponse = mock(HttpResponse.class);
        llmGateway.setHttpClient(mockHttpClient);
        aiStrategy = new AiStrategy(llmGateway, fallbackStrategy);
    }

    private Product createProduct() {
        Product p = new Product();
        p.setId("PRD-1");
        p.setName("Test Product");
        p.setCategory(Category.APPAREL);
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setStockLevel(5);
        p.setReorderThreshold(10);
        p.setDemandVelocity(5);
        return p;
    }

    @Test
    void testValidGeminiResponse() throws Exception {
        Product p = createProduct();
        String jsonResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"" +
            "{\\\"pricing\\\":{\\\"recommendedPrice\\\":120.00,\\\"direction\\\":\\\"INCREASE\\\",\\\"confidence\\\":0.9,\\\"reasoning\\\":\\\"reason\\\"}," +
            "\\\"reorder\\\":{\\\"recommendedQuantity\\\":20,\\\"confidence\\\":0.8,\\\"reasoning\\\":\\\"reason\\\"}}" +
            "\"}]}}]}";

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonResponse);

        SuggestionPair pair = aiStrategy.generateSuggestions(p, TriggerReason.INVENTORY_LOW, 5);

        assertNotNull(pair);
        assertEquals(new BigDecimal("120.00"), pair.getPricing().getRecommendedPrice());
        assertEquals(PricingDirection.INCREASE, pair.getPricing().getDirection());
        assertEquals(20, pair.getReorder().getRecommendedQuantity());
        verify(fallbackStrategy, never()).generateSuggestions(any(), any(), anyInt());
    }

    @Test
    void testInvalidJsonTriggersFallback() throws Exception {
        Product p = createProduct();
        String invalidJson = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"not json\"}]}}]}";

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(invalidJson);

        SuggestionPair fallbackPair = new SuggestionPair(null, null);
        when(fallbackStrategy.generateSuggestions(any(), any(), anyInt())).thenReturn(fallbackPair);

        SuggestionPair pair = aiStrategy.generateSuggestions(p, TriggerReason.INVENTORY_LOW, 5);

        assertSame(fallbackPair, pair);
        verify(fallbackStrategy, times(1)).generateSuggestions(any(), any(), anyInt());
    }

    @Test
    void testNegativePriceTriggersFallback() throws Exception {
        Product p = createProduct();
        String jsonResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"" +
            "{\\\"pricing\\\":{\\\"recommendedPrice\\\":-10.00,\\\"direction\\\":\\\"DECREASE\\\",\\\"confidence\\\":0.9,\\\"reasoning\\\":\\\"reason\\\"}," +
            "\\\"reorder\\\":{\\\"recommendedQuantity\\\":20,\\\"confidence\\\":0.8,\\\"reasoning\\\":\\\"reason\\\"}}" +
            "\"}]}}]}";

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonResponse);

        SuggestionPair fallbackPair = new SuggestionPair(null, null);
        when(fallbackStrategy.generateSuggestions(any(), any(), anyInt())).thenReturn(fallbackPair);

        SuggestionPair pair = aiStrategy.generateSuggestions(p, TriggerReason.INVENTORY_LOW, 5);
        assertSame(fallbackPair, pair);
    }

    @Test
    void testZeroReorderTriggersFallback() throws Exception {
        Product p = createProduct();
        String jsonResponse = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"" +
            "{\\\"pricing\\\":{\\\"recommendedPrice\\\":120.00,\\\"direction\\\":\\\"INCREASE\\\",\\\"confidence\\\":0.9,\\\"reasoning\\\":\\\"reason\\\"}," +
            "\\\"reorder\\\":{\\\"recommendedQuantity\\\":0,\\\"confidence\\\":0.8,\\\"reasoning\\\":\\\"reason\\\"}}" +
            "\"}]}}]}";

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonResponse);

        SuggestionPair fallbackPair = new SuggestionPair(null, null);
        when(fallbackStrategy.generateSuggestions(any(), any(), anyInt())).thenReturn(fallbackPair);

        SuggestionPair pair = aiStrategy.generateSuggestions(p, TriggerReason.INVENTORY_LOW, 5);
        assertSame(fallbackPair, pair);
    }
    
    @Test
    void testMissingApiKeyTriggersFallback() {
        LLMGateway noAuthGateway = new LLMGateway("", new ObjectMapper());
        AiStrategy unauthAiStrategy = new AiStrategy(noAuthGateway, fallbackStrategy);
        
        SuggestionPair fallbackPair = new SuggestionPair(null, null);
        when(fallbackStrategy.generateSuggestions(any(), any(), anyInt())).thenReturn(fallbackPair);

        SuggestionPair pair = unauthAiStrategy.generateSuggestions(createProduct(), TriggerReason.INVENTORY_LOW, 5);
        assertSame(fallbackPair, pair);
        verify(fallbackStrategy, times(1)).generateSuggestions(any(), any(), anyInt());
    }
}
