package com.stockpulse.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.domain.*;
import com.stockpulse.service.strategy.SuggestionPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class LLMGateway {
    private static final Logger logger = LoggerFactory.getLogger(LLMGateway.class);

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public LLMGateway(@Value("${GEMINI_API_KEY:}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // For testing
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public SuggestionPair generateSuggestions(Product product, TriggerReason triggerReason, int categoryAvgVelocity) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API key is missing");
        }

        String prompt = buildPrompt(product, triggerReason, categoryAvgVelocity);
        String requestJson = buildGeminiRequest(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API returned error: " + response.statusCode() + " " + response.body());
        }

        GeminiResponseDto geminiResponse = objectMapper.readValue(response.body(), GeminiResponseDto.class);
        String text = geminiResponse.extractText();
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("Gemini returned empty response");
        }

        // Gemini sometimes wraps JSON in markdown code blocks like ```json ... ```
        if (text.startsWith("```json")) {
            text = text.substring(7);
        }
        if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        LLMResponseDto llmResponse = objectMapper.readValue(text, LLMResponseDto.class);
        validateResponse(llmResponse);

        return convertToSuggestionPair(product, triggerReason, llmResponse);
    }

    private String buildPrompt(Product p, TriggerReason triggerReason, int categoryAvgVelocity) {
        String baseContext = String.format(
            "Product Name: %s\nCategory: %s\nCurrent Price: %s\nCurrent Stock: %d\nReorder Threshold: %d\nDemand Velocity: %d\nCategory Avg Velocity: %d\nTrigger Reason: %s\n\n",
            p.getName(), p.getCategory(), p.getCurrentPrice(), p.getStockLevel(), p.getReorderThreshold(), p.getDemandVelocity(), categoryAvgVelocity, triggerReason
        );

        String specificInstruction;
        if (triggerReason == TriggerReason.INVENTORY_LOW) {
            specificInstruction = "Focus on the low inventory risk. The stock has fallen below the reorder threshold. Recommend a price increase to slow down sales if necessary, and recommend an immediate stock replenishment amount to avoid a stockout. Explain your reasoning clearly.";
        } else {
            specificInstruction = "Focus on the unusual demand increase. The demand velocity is significantly higher than the category average. Recommend a strategic price increase to capture better margins, and recommend a stock replenishment amount to sustain the sales momentum. Explain your reasoning clearly.";
        }

        String jsonInstruction = "\n\nYou are advising an e-commerce merchandising team. Return ONLY structured JSON exactly matching this format, with no extra text or markdown:\n" +
            "{\n" +
            "  \"pricing\": {\n" +
            "    \"recommendedPrice\": 12.99,\n" +
            "    \"direction\": \"INCREASE\" or \"DECREASE\" or \"HOLD\",\n" +
            "    \"confidence\": 0.95,\n" +
            "    \"reasoning\": \"Brief explanation...\"\n" +
            "  },\n" +
            "  \"reorder\": {\n" +
            "    \"recommendedQuantity\": 50,\n" +
            "    \"confidence\": 0.95,\n" +
            "    \"reasoning\": \"Brief explanation...\"\n" +
            "  }\n" +
            "}\n" +
            "Do not invent missing product data. Never return negative prices or quantities. Confidence must be between 0 and 1.";

        return baseContext + specificInstruction + jsonInstruction;
    }

    private String buildGeminiRequest(String prompt) throws Exception {
        // Simple JSON builder for the request structure to avoid creating many classes
        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");
        return "{\n" +
            "  \"contents\": [{\n" +
            "    \"parts\": [{\"text\": \"" + escapedPrompt + "\"}]\n" +
            "  }],\n" +
            "  \"generationConfig\": {\n" +
            "    \"responseMimeType\": \"application/json\"\n" +
            "  }\n" +
            "}";
    }

    private void validateResponse(LLMResponseDto res) {
        if (res.getPricing() == null || res.getReorder() == null) {
            throw new IllegalArgumentException("Missing pricing or reorder block");
        }
        
        if (res.getPricing().getRecommendedPrice() == null || res.getPricing().getRecommendedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid recommended price");
        }
        if (res.getPricing().getDirection() == null) {
            throw new IllegalArgumentException("Missing pricing direction");
        }
        if (res.getPricing().getConfidence() == null || res.getPricing().getConfidence() < 0 || res.getPricing().getConfidence() > 1) {
            throw new IllegalArgumentException("Invalid pricing confidence");
        }
        if (res.getPricing().getReasoning() == null || res.getPricing().getReasoning().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing pricing reasoning");
        }

        if (res.getReorder().getRecommendedQuantity() == null || res.getReorder().getRecommendedQuantity() < 1) {
            throw new IllegalArgumentException("Invalid recommended quantity");
        }
        if (res.getReorder().getConfidence() == null || res.getReorder().getConfidence() < 0 || res.getReorder().getConfidence() > 1) {
            throw new IllegalArgumentException("Invalid reorder confidence");
        }
        if (res.getReorder().getReasoning() == null || res.getReorder().getReasoning().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing reorder reasoning");
        }
    }

    private SuggestionPair convertToSuggestionPair(Product p, TriggerReason triggerReason, LLMResponseDto res) {
        PricingSuggestion pricing = new PricingSuggestion();
        pricing.setProduct(p);
        pricing.setCurrentPrice(p.getCurrentPrice());
        pricing.setRecommendedPrice(res.getPricing().getRecommendedPrice().setScale(2, RoundingMode.HALF_UP));
        pricing.setDirection(PricingDirection.valueOf(res.getPricing().getDirection().toUpperCase()));
        pricing.setConfidence(res.getPricing().getConfidence());
        pricing.setReasoning(res.getPricing().getReasoning());
        pricing.setStatus(SuggestionStatus.PENDING);
        pricing.setTriggerReason(triggerReason);

        ReorderSuggestion reorder = new ReorderSuggestion();
        reorder.setProduct(p);
        reorder.setCurrentStock(p.getStockLevel());
        reorder.setRecommendedQuantity(res.getReorder().getRecommendedQuantity());
        reorder.setSuggestedLeadTimeDays(7); // Default
        reorder.setConfidence(res.getReorder().getConfidence());
        reorder.setReasoning(res.getReorder().getReasoning());
        reorder.setStatus(SuggestionStatus.PENDING);
        reorder.setTriggerReason(triggerReason);

        return new SuggestionPair(pricing, reorder);
    }
}
